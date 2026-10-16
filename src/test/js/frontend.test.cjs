const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const crypto = require('node:crypto');
const root = 'src/main/resources/static/';
const html = fs.readFileSync(root + 'index.html', 'utf8');
const appSource = fs.readFileSync(root + 'js/app.js', 'utf8');
const apiSource = fs.readFileSync(root + 'js/api.js', 'utf8');
const prototypeHtml = fs.readFileSync('index.html', 'utf8');
const prototypeAppSource = fs.readFileSync('js/app.js', 'utf8');
const settle = async () => { for (let i = 0; i < 12; i++) await new Promise(resolve => setImmediate(resolve)); };

// DOM minimo en memoria: ejecuta los manejadores reales, sin navegador ni MySQL.
function ui({ authenticated = false, role = 'ADMINISTRADOR' } = {}) {
    const nodes = new Map(), calls = [], listeners = new Map(), storage = [];
    const fixture = { lote: { codigo:'REAL001',productor:'Productor <prueba>',origen:'Rivera',fecha:'2026-10-16',estadoLote:'PENDIENTE',unidad:'kg' },
        recibidoKg:100,consumidoKg:20,disponibleKg:80,salidaProducidaKg:16,operaciones:[] };
    class Element {
        constructor(id) { this.id=id; this.value=''; this.dataset={}; this.handlers={}; this.hidden=false;
            const names=new Set(); this.classList={add:n=>names.add(n),remove:n=>names.delete(n),contains:n=>names.has(n),
                toggle:(n,b)=>{if(b===undefined)b=!names.has(n);b?names.add(n):names.delete(n);}};
            this.style={}; this.textContent=''; this._html=''; }
        set innerHTML(v) { this._html=v; for(const m of v.matchAll(/id="([^"]+)"/g)) if(!nodes.has(m[1]))nodes.set(m[1],new Element(m[1])); }
        get innerHTML() {return this._html;}
        addEventListener(name,handler){(this.handlers[name]??=[]).push(handler);}
        setAttribute(name,value){this[name]=value;}
        fire(name,event={}){for(const h of this.handlers[name]||[])h({preventDefault(){},target:this,...event});}
        querySelectorAll(){return [];}
        replaceChildren(){this._html='';this.textContent='';}
        append(...parts){this.textContent+=parts.map(p=>typeof p==='string'?p:p.textContent).join('');}
        reset(){}
        click(){this.fire('click');}
        play(){return Promise.resolve();}
    }
    for(const m of html.matchAll(/id="([^"]+)"/g))nodes.set(m[1],new Element(m[1]));
    const pages=['dashboard','lots','transform','trace','public','alerts','audit'];
    const nav=pages.map(p=>{const e=new Element(p);e.dataset.page=p;return e;});
    const roleChoices=['Administrador','Operador'].map(role=>{const e=new Element(role);e.dataset.loginRole=role;return e;});
    const classNodes=new Map(['.sidebar','.landing-video'].map(k=>[k,new Element(k)]));
    const articles=Array.from({length:4},(_,i)=>new Element('stat'+i));
    function all(selector) {
        if(selector==='.side-link')return nav;
        if(selector==='.app-page')return pages.map(p=>nodes.get('page-'+p));
        if(selector==='.modal')return [...nodes.values()].filter(n=>n.id.endsWith('Modal'));
        if(selector==='#page-dashboard .stat-grid article')return articles;
        if(selector==='[data-login-role]')return roleChoices;
        const match=selector.match(/^\[data-page="(.*)"\]$/); if(match)return nav.filter(n=>n.dataset.page===match[1]);
        return [];
    }
    const get = selector => selector.startsWith('#') ? nodes.get(selector.slice(1)) : classNodes.get(selector);
    Object.assign(nodes.get('transformInput'),{value:'100'}); Object.assign(nodes.get('transformOutput'),{value:'80'});
    Object.assign(nodes.get('transformCost'),{value:'1600000'}); Object.assign(nodes.get('salePrice'),{value:'25000'});
    nodes.get('transformLot').value='REAL001'; nodes.get('traceCode').value='REAL001'; nodes.get('publicCode').value='REAL001';
    nodes.get('transformCosts').value='Mano de obra'; nodes.get('transformBalance').value='no';
    const account={correo:'test@example.invalid',permisos:['ROLE_'+role]};
    const request=async(path,options={})=>{
        calls.push({path,options});
        if(path==='/api/cuenta'){if(!authenticated)throw Object.assign(new Error('Sin sesión'),{status:401});return account;}
        if(path==='/api/lotes' && options.method==='POST')return {codigo:options.body.codigo};
        if(path==='/api/lotes')return [fixture.lote];
        if(path.endsWith('/trazabilidad'))return fixture;
        if(path.endsWith('/estado'))return fixture.lote;
        if(path.endsWith('/auditoria'))return [];
        if(path==='/api/operaciones')return {mermaPorcentaje:20,alertaMerma:true,causaAlerta:'Supera 15%',costoUnitarioCopKg:20000,margenSobreVentaPorcentaje:20};
        if(path.startsWith('/api/publico/productos/'))return {codigo:'REAL001',origen:'Rivera',fechaRecepcion:'2026-10-16',cantidadRecibida:100,unidad:'kg',estado:'BLOQUEADO',disponibleParaVenta:false,trazabilidad:[]};
        throw new Error('Peticion inesperada: '+path);
    };
    const window={CacaoApi:{request,login:async(correo,password)=>{calls.push({login:{correo,password}});authenticated=true;return account;},logout:async()=>{calls.push({path:'/api/logout'});authenticated=false;}},
        addEventListener:(name,fn)=>listeners.set(name,fn),scrollTo(){},innerWidth:1200};
    let promptCount=0;
    vm.runInNewContext(appSource,{window,document:{querySelector:get,querySelectorAll:all,createElement:()=>new Element('created')},
        localStorage:{removeItem:k=>storage.push(['remove',k]),setItem:()=>{throw new Error('No guardar credenciales');}},
        location:{origin:'http://localhost',search:''}, URL, URLSearchParams,Intl,Date,Math,JSON,Number,String,Promise,crypto,
        setTimeout:()=>0,clearTimeout(){},prompt:()=> (++promptCount % 2 ? 'Materia prima y trabajo':'2026-10-16'),confirm:()=>false,console});
    return {nodes,calls,storage,listeners,nav,fixture,articles,roleChoices};
}

test('inicio anonimo no solicita trazabilidad privada ni carga lotes demo',async()=>{
    const x=ui();await settle();assert.deepEqual(x.calls.map(c=>c.path),['/api/cuenta']);
    assert.equal(x.nodes.get('statLots').textContent,'00');assert.equal(x.storage.length,1);
});
test('el acceso no ofrece credenciales demo ni permite autenticacion local simulada',()=>{
    assert.doesNotMatch(html,/demo-credentials|demo@cacaovivo/i);
    assert.match(html,/data-login-role="Administrador"/);
    assert.match(html,/data-login-role="Operador"/);
    assert.match(html,/El servidor determina tus permisos/);
    assert.doesNotMatch(prototypeHtml,/demo-credentials|data-login-role|demo@cacaovivo|CacaoDemo|OperadorDemo/i);
    assert.doesNotMatch(prototypeAppSource,/const accounts\s*=/);
    assert.match(prototypeHtml,/no autentica usuarios ni acepta credenciales de demostración/i);
});
test('login usa rol del servidor y borra el campo de contraseña',async()=>{
    const x=ui({role:'OPERADOR'});await settle();
    x.roleChoices[1].click();
    assert.equal(x.roleChoices[1].classList.contains('selected'),true);
    assert.equal(x.roleChoices[1]['aria-pressed'],'true');
    x.nodes.get('loginEmail').value='test@example.invalid';x.nodes.get('loginPassword').value='clave-solo-prueba';
    x.nodes.get('loginForm').fire('submit');await settle();
    assert.equal(x.nodes.get('loginPassword').value,'');assert.equal(x.nodes.get('userMenu').textContent,'OP');
    assert.match(x.nodes.get('toast').textContent,/servidor/);
    assert.equal(x.nodes.get('exportCsv').hidden,true);assert.match(x.nodes.get('lotsTable').innerHTML,/REAL001/);
    assert.match(x.nodes.get('lotsTable').innerHTML,/Productor &lt;prueba&gt;/);
});
test('cierre de sesión está visible y limpia el estado de la interfaz',async()=>{
    const x=ui({authenticated:true});await settle();
    assert.equal(x.nodes.get('logoutButton').hidden,false);
    x.nodes.get('logoutButton').click();await settle();
    assert.equal(x.calls.some(c=>c.path==='/api/logout'),true);
    assert.equal(x.nodes.get('logoutButton').hidden,true);
    assert.equal(x.nodes.get('statLots').textContent,'00');
    assert.doesNotMatch(x.nodes.get('lotsTable').innerHTML,/REAL001/);
    assert.equal(x.nodes.get('loginPassword').value,'');
});
test('recupera sesion y consulta trazabilidad al navegar',async()=>{
    const x=ui({authenticated:true});await settle();x.nav.find(n=>n.dataset.page==='trace').click();await settle();
    assert.match(x.nodes.get('traceOutput').innerHTML,/REAL001/);assert.match(x.nodes.get('traceOutput').innerHTML,/Rivera/);
});
test('registro de lote envia las notas y campos reales a la API',async()=>{
    const x=ui({authenticated:true});await settle();
    for(const [key,value]of Object.entries({newCode:'NUEVO',newProducer:'Persona',newOrigin:'Neiva',newDate:'2026-10-16',newQty:'25',newNotes:'Nota'}))x.nodes.get(key).value=value;
    x.nodes.get('lotForm').fire('submit');await settle();
    const saved=x.calls.find(c=>c.path==='/api/lotes'&&c.options.method==='POST');
    assert.equal(saved.options.body.cantidad,25);assert.equal(saved.options.body.observacion,'Nota');
});
test('reenvio de formulario conserva codigo idempotente',async()=>{
    const x=ui({authenticated:true});await settle();x.nodes.get('transformStage').value='Secado';
    x.nodes.get('transformForm').fire('submit');await settle();x.nodes.get('transformForm').fire('submit');await settle();
    const operations=x.calls.filter(c=>c.path==='/api/operaciones');assert.equal(operations.length,2);
    assert.equal(operations[0].options.body.codigoOperacion,operations[1].options.body.codigoOperacion);
    assert.equal(x.nodes.get('calcMerma').textContent,'20%');
});
test('simulador proyecta rendimiento y rentabilidad sin enviar ni persistir cambios',async()=>{
    const x=ui({authenticated:true});await settle();
    const callsBefore=x.calls.length;
    x.nodes.get('scenarioLot').value='REAL001';
    x.nodes.get('scenarioWaste').value='25';
    x.nodes.get('scenarioPrice').value='30000';
    x.nodes.get('scenarioCost').value='10000';
    x.nodes.get('scenarioWaste').fire('input');
    assert.equal(x.nodes.get('scenarioYield').textContent,'75%');
    assert.equal(x.nodes.get('scenarioOutput').textContent,'60 kg');
    assert.match(x.nodes.get('scenarioRevenue').textContent,/1\.800\.000 COP/);
    assert.match(x.nodes.get('scenarioTotalCost').textContent,/800\.000 COP/);
    assert.match(x.nodes.get('scenarioProfit').textContent,/1\.000\.000 COP/);
    assert.equal(x.nodes.get('scenarioMargin').textContent,'55,56%');
    assert.equal(x.nodes.get('scenarioResults').hidden,false);
    assert.equal(x.calls.length,callsBefore);
    assert.equal(x.calls.some(c=>c.path==='/api/operaciones'),false);
    assert.equal(x.storage.some(entry=>entry[0]==='set'),false);
});
test('costos obligatorios muestran error persistente sin enviar la operacion',async()=>{
    const x=ui({authenticated:true});await settle();
    x.nodes.get('transformStage').value='Secado';x.nodes.get('transformCosts').value='';
    x.nodes.get('transformForm').fire('submit');await settle();
    assert.equal(x.calls.filter(c=>c.path==='/api/operaciones').length,0);
    assert.match(x.nodes.get('transformFeedback').textContent,/costos/);
});
test('cambio de masa requiere justificacion y evita calcular merma simple',async()=>{
    const x=ui({authenticated:true});await settle();
    x.nodes.get('transformStage').value='Secado';x.nodes.get('transformBalance').value='si';
    x.nodes.get('transformBalance').fire('change');
    assert.equal(x.nodes.get('balanceReasonLabel').hidden,false);
    assert.equal(x.nodes.get('calcMerma').textContent,'No comparable');
    x.nodes.get('transformForm').fire('submit');await settle();
    assert.equal(x.calls.filter(c=>c.path==='/api/operaciones').length,0);
    x.nodes.get('transformBalanceReason').value='Ajuste de humedad';
    x.nodes.get('transformForm').fire('submit');await settle();
    assert.equal(x.calls.find(c=>c.path==='/api/operaciones').options.body.justificacionBalance,'Ajuste de humedad');
});
test('formularios no usan dialogos nativos ni cantidades precargadas',()=>{
    assert.doesNotMatch(appSource,/\b(prompt|confirm|alert)\s*\(/);
    assert.doesNotMatch(html,/id="transform(Input|Output|Cost)"[^>]*\bvalue=/);
});
test('RF06 consulta exclusivamente endpoint publico y no muestra productor',async()=>{
    const x=ui();await settle();x.nodes.get('landingCode').value='REAL001';x.nodes.get('landingLookup').fire('submit');await settle();
    assert.match(x.nodes.get('landingResult').innerHTML,/Disponible para venta: No/);
    assert.doesNotMatch(x.nodes.get('landingResult').innerHTML,/Productor/);
});
test('expiracion limpia datos de la cuenta previa',async()=>{
    const x=ui({authenticated:true});await settle();x.listeners.get('cacao:session-expired')();
    assert.equal(x.nodes.get('statLots').textContent,'00');assert.doesNotMatch(x.nodes.get('lotsTable').innerHTML,/REAL001/);
});
test('cliente API rota CSRF y usa cookies sin almacenar credenciales',async()=>{
    const calls=[];let token=0;
    const sandbox={window:{},Event,URLSearchParams,fetch:async(path,options)=>{
        calls.push({path,options});
        return {ok:true,status:path==='/api/login'?204:200,json:async()=>path==='/api/csrf'?{token:'token-'+(++token),headerName:'X-CSRF-TOKEN'}:{correo:'prueba'}};
    }};
    vm.runInNewContext(apiSource,sandbox);
    await sandbox.window.CacaoApi.login('prueba','clave-temporal');
    await sandbox.window.CacaoApi.request('/api/lotes',{method:'POST',body:{codigo:'A'}});
    assert.equal(calls.find(c=>c.path==='/api/login').options.headers['X-CSRF-TOKEN'],'token-1');
    assert.equal(calls.at(-1).options.headers['X-CSRF-TOKEN'],'token-2');
    assert.equal(calls.at(-1).options.credentials,'same-origin');
    assert.equal(calls.at(-1).options.headers.Authorization,undefined);
});
