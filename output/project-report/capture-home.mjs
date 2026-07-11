import { spawn } from 'node:child_process';
import fs from 'node:fs/promises';
const edge = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const userData = 'C:/Users/34062/Documents/VsCode/code/ToveloKno/ToveloKno/output/project-report/edge-profile-cdp';
await fs.mkdir(userData, { recursive: true });
const proc = spawn(edge, [
  '--headless=new', '--disable-gpu', '--remote-debugging-port=9223', `--user-data-dir=${userData}`,
  '--window-size=1365,768', 'http://127.0.0.1:5173/'
], { stdio: 'ignore' });
async function sleep(ms){ return new Promise(r=>setTimeout(r,ms)); }
async function json(url, options){ const r = await fetch(url, options); return r.json(); }
let page;
for (let i=0;i<60;i++) {
  try {
    const list = await json('http://127.0.0.1:9223/json');
    page = list.find(p => p.type === 'page');
    if (page) break;
  } catch {}
  await sleep(250);
}
if (!page) throw new Error('No CDP page');
const ws = new WebSocket(page.webSocketDebuggerUrl);
let id = 0; const pending = new Map();
ws.onmessage = ev => { const msg = JSON.parse(ev.data); if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); } };
await new Promise((resolve, reject)=>{ ws.onopen=resolve; ws.onerror=reject; });
function send(method, params={}) { const mid=++id; ws.send(JSON.stringify({id:mid, method, params})); return new Promise(r=>pending.set(mid,r)); }
await send('Runtime.enable'); await send('Page.enable');
await send('Runtime.evaluate', { expression: `localStorage.setItem('tovelokno_token', '');`, awaitPromise: true });
const loginResp = await fetch('http://127.0.0.1:8080/api/users/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username:'reportdemo', password:'reportdemo123'}) });
const loginData = await loginResp.json();
const token = loginData.data.token;
await send('Runtime.evaluate', { expression: `localStorage.setItem('tovelokno_token', ${JSON.stringify(token)}); location.href='http://127.0.0.1:5173/';`, awaitPromise: true });
await sleep(6000);
await send('Runtime.evaluate', { expression: `window.scrollTo(0,0);`, awaitPromise: true });
await sleep(1200);
const shot = await send('Page.captureScreenshot', { format:'png', captureBeyondViewport:false });
await fs.writeFile('C:/Users/34062/Documents/VsCode/code/ToveloKno/ToveloKno/output/project-report/home-screenshot.png', Buffer.from(shot.result.data, 'base64'));
ws.close(); proc.kill();
