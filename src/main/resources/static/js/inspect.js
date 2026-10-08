/* 智能检测页 */
let selectedFile = null;
let currentRecordId = null;
let currentDefects = [];

const drop = document.getElementById('drop');
const fileInput = document.getElementById('fileInput');
drop.addEventListener('click', () => fileInput.click());
drop.addEventListener('dragover', e => { e.preventDefault(); drop.classList.add('over'); });
drop.addEventListener('dragleave', () => drop.classList.remove('over'));
drop.addEventListener('drop', e => {
  e.preventDefault(); drop.classList.remove('over');
  if (e.dataTransfer.files.length) setFile(e.dataTransfer.files[0]);
});
fileInput.addEventListener('change', () => { if (fileInput.files.length) setFile(fileInput.files[0]); });

function setFile(f) {
  selectedFile = f;
  document.getElementById('inspectBtn').disabled = false;
  drop.querySelector('div').textContent = '📷 已选择：' + f.name;
}

async function doInspect() {
  if (!selectedFile) return;
  const tip = document.getElementById('inspectTip');
  tip.textContent = 'AI 检测中（VLM 约 3~15 秒，Mock 秒回）…';
  document.getElementById('inspectBtn').disabled = true;
  document.getElementById('resultCard').style.display = 'none';
  try {
    const fd = new FormData();
    fd.append('file', selectedFile);
    fd.append('productId', document.getElementById('productSel').value);
    fd.append('batchNo', document.getElementById('batchNo').value.trim());
    const j = await api('/api/inspect', {method: 'POST', body: fd});
    if (j.code !== 0) { toast(j.msg); return; }
    showResult(j.data);
  } catch (e) { toast('检测失败：' + e.message); }
  finally { tip.textContent = ''; document.getElementById('inspectBtn').disabled = false; }
}

function showResult(r) {
  currentRecordId = r.id;
  const result = JSON.parse(r.aiResultJson || '{}');
  currentDefects = result.defects || [];
  document.getElementById('resultCard').style.display = 'block';
  document.getElementById('resultBadge').innerHTML =
    r.overallResult === 'PASS' ? '<span class="badge pass">合格 PASS</span>' : '<span class="badge fail">不合格 FAIL</span>';
  document.getElementById('srcBadge').textContent = r.source + ' 模式';
  document.getElementById('latencyLine').textContent =
    `来源 ${r.source} · 耗时 ${r.latencyMs}ms · 记录 #${r.id}`;
  const img = document.getElementById('previewImg');
  img.onload = () => drawBboxes(document.getElementById('bboxCanvas'), img, currentDefects);
  img.src = '/' + r.imagePath;
  const list = document.getElementById('defectList');
  if (!currentDefects.length) {
    list.innerHTML = '<div class="defect low"><div class="t">✓ 未发现缺陷</div><div class="meta">AI 判定合格，等待人工复核确认</div></div>';
  } else {
    list.innerHTML = currentDefects.map((d, i) => `
      <div class="defect ${d.severity}">
        <div class="t">${i + 1}. ${typeCn(d.type)} <span class="badge ${d.severity === 'high' ? 'fail' : d.severity === 'low' ? 'pass' : 'wait'}">${sevCn(d.severity)}</span></div>
        <div class="meta">置信度 ${(d.confidence * 100).toFixed(0)}% · 处置建议：${d.suggestion || '—'} · ${d.description || ''}</div>
      </div>`).join('');
  }
  document.getElementById('resultCard').scrollIntoView({behavior: 'smooth'});
}

async function confirmResult(verdict) {
  if (!currentRecordId) return;
  const note = document.getElementById('confirmNote').value.trim();
  const j = await api(`/api/records/${currentRecordId}/confirm`, {
    method: 'POST', headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({verdict, note})
  });
  if (j.code === 0) {
    toast(`复核完成：${verdict === 'PASS' ? '合格' : '不合格'}`);
    document.getElementById('confirmNote').value = '';
  } else toast(j.msg);
}
