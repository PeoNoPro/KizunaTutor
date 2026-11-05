import { api, byId, text, date, notice, emptyState, createOperationsPage } from '../../js/pages/operations.js';
import { reloadIfInsideLearnerShell, mountLearnerFrameIfLearner } from '../../js/pages/learner-frame.js';

async function loadPolicies() {
    const rows = await api('/policies'); byId('policyList').replaceChildren();
    if (!rows.length) {
        notice('Chưa có tài liệu được công bố và có hiệu lực.');
        byId('policyList').append(emptyState('Chưa có tài liệu được công bố', 'Các chính sách có hiệu lực sẽ hiện ở đây khi được công bố.'));
        return;
    }
    rows.forEach(row => {
        const card = text('article', '', 'operations-card'); const link = text('a', row.title);
        link.href = '/pages/policy-document/?type=' + encodeURIComponent(row.type);
        card.append(link, text('p', 'Phiên bản ' + row.versionLabel + ' · Hiệu lực ' + date(row.effectiveAt))); byId('policyList').append(card);
    }); notice('Chọn chính sách bạn muốn đọc.');
}

if (!reloadIfInsideLearnerShell()) {
    mountLearnerFrameIfLearner('policies');
    createOperationsPage(loadPolicies).start();
}
