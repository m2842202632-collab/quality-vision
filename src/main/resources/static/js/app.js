/* 公共：鉴权、导航、toast、API 封装 */
let username = '';

async function api(path, opts = {}) {
  const r = await fetch(path, opts);
  if (r.status === 401) { location.href = '/login.html'; throw new Error('未登录'); }
  return r.json();
}

function toast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.style.display = 'block';
  setTimeout(() => t.style.display = 'none', 2600);
}

function nav(page) {
  document.querySelectorAll('.nav button').forEach(b => b.classList.toggle('active', b.dataset.page === page));
  document.querySelectorAll('.page').forEach(p => p.classList.toggle('active', p.id === 'page-' + page));
  if (page === 'records') loadRecords(1);
  if (page === 'dashboard') loadDashboard();
  if (page === 'products') loadProductsPage();
}

async function logout() {
  await fetch('/api/auth/logout', {method: 'POST'});
  location.href = '/login.html';
}

/* 缺陷类型/严重度中英文映射 */
const TYPE_CN = {scratch:'划痕', crack:'裂纹', stain:'污点', deformation:'变形', missing_part:'缺件', hole:'破洞', earloop_defect:'耳带缺陷', seal_defect:'封口缺陷', unknown:'未知类型'};
const SEV_CN = {high:'严重', medium:'中等', low:'轻微'};
const typeCn = t => TYPE_CN[t] || t;
const sevCn = s => SEV_CN[s] || s;

/* 在 canvas 上按 0-1000 归一化坐标画缺陷框 */
function drawBboxes(canvas, img, defects) {
  const w = img.clientWidth, h = img.clientHeight;
  canvas.width = w; canvas.height = h;
  canvas.style.width = w + 'px'; canvas.style.height = h + 'px';
  const ctx = canvas.getContext('2d');
  ctx.lineWidth = 2;
  (defects || []).forEach((d, i) => {
    const [x1, y1, x2, y2] = d.bbox;
    const color = d.severity === 'high' ? '#f87171' : d.severity === 'low' ? '#34d399' : '#fbbf24';
    ctx.strokeStyle = color;
    ctx.strokeRect(x1 / 1000 * w, y1 / 1000 * h, (x2 - x1) / 1000 * w, (y2 - y1) / 1000 * h);
    ctx.fillStyle = color;
    ctx.font = '12px sans-serif';
    ctx.fillText(`${i + 1}. ${typeCn(d.type)}`, x1 / 1000 * w + 4, y1 / 1000 * h + 14);
  });
}

window.addEventListener('DOMContentLoaded', async () => {
  try {
    const j = await api('/api/auth/me');
    if (j.code === 0) { username = j.data.username; document.getElementById('who').textContent = '👤 ' + username; }
  } catch (e) { /* 未登录会被 api() 跳转 */ }
  loadProductOptions();
});

async function loadProductOptions() {
  const j = await api('/api/products');
  if (j.code !== 0) return;
  const opts = j.data.map(p => `<option value="${p.id}">${p.name}（${p.code}）</option>`).join('');
  for (const id of ['productSel', 'fProduct', 'sProduct']) {
    const el = document.getElementById(id);
    if (el) el.innerHTML = (id === 'productSel' ? '' : '<option value="">全部产品</option>') + opts;
  }
}
