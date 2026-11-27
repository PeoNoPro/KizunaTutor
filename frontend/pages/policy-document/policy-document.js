import { api, byId, text, date, notice, createOperationsPage } from '../../js/pages/operations.js';
import { mountLearnerFrameIfLearner } from '../../js/pages/learner-frame.js';

const policyType = new URLSearchParams(location.search).get('type') || '';

async function loadPolicy() {
    const data = await api('/policies/' + encodeURIComponent(policyType));
    byId('policyDetail').replaceChildren(text('h2', data.title), text('p', 'Phiên bản ' + data.versionLabel + ' · Hiệu lực ' + date(data.effectiveAt)), text('p', data.content));
    notice('Đang hiển thị bản đã công bố và có hiệu lực.');
}

mountLearnerFrameIfLearner('policies');
createOperationsPage(loadPolicy).start();
