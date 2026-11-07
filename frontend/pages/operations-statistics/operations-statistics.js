import { OPERATIONS_BASE as base, api, byId, text, date, notice, query, createOperationsPage } from '../../js/pages/operations.js';
import { barChart, columnChart, chartError, bindChartTooltip } from '../../js/pages/admin-charts.js';

const number = value => new Intl.NumberFormat('vi-VN').format(value);
const day = value => value.split('-').reverse().join('/');

function failed(ids, message) {
    ids.forEach(id => chartError(byId(id), message));
}

async function loadAccounts(params) {
    try {
        const data = await api(base + '/dashboard/account-statistics?' + params);
        const form = byId('statisticsFilters').elements;
        form.startDate.value = data.startDate; form.endDate.value = data.endDate;
        byId('statisticsPeriod').textContent = 'Kỳ ' + day(data.startDate) + ' đến ' + day(data.endDate) + ' · Cập nhật: ' + date(data.generatedAt) + '. Tối đa 366 ngày theo giờ Việt Nam.';
        columnChart(byId('newAccountsChart'), data.newAccounts, {headline: 'tài khoản mới trong kỳ', empty: 'Không có tài khoản mới trong kỳ đã chọn.', caption: 'Tài khoản mới trong kỳ', format: value => number(value)});
        barChart(byId('rolesChart'), 'Tài khoản theo vai trò', data.roles, 'Chưa có tài khoản trong hệ thống.');
        barChart(byId('statusesChart'), 'Trạng thái tài khoản', data.statuses, 'Chưa có tài khoản trong hệ thống.');
    } catch (error) {
        if (error.status === 401) throw error;
        // Lỗi không hiển thị thành số 0 để không bị hiểu nhầm là chưa có tài khoản.
        failed(['newAccountsChart', 'rolesChart', 'statusesChart'], error.status === 403 ? error.message : 'Chưa tải được số liệu tài khoản.');
        return error;
    }
}

const labelled = groups => Object.entries(groups).map(([label, count]) => ({label, count}));

async function loadIncidents(params) {
    try {
        const data = await api(base + '/reports/incidents?' + params);
        byId('incidentTiles').replaceChildren(...[['Phát sinh trong kỳ', data.createdCount], ['Khắc phục trong kỳ', data.resolvedCount], ['Còn mở cuối kỳ', data.openAtCutoff]].map(([label, count]) => {
            const tile = text('div', '', 'statistics-tile'); tile.append(text('span', label), text('strong', number(count))); return tile;
        }));
        barChart(byId('incidentStatusChart'), 'Sự cố chưa đóng theo bước xử lý', labelled(data.breakdowns.open.status), 'Không có sự cố còn mở cuối kỳ.');
        barChart(byId('incidentSeverityChart'), 'Sự cố chưa đóng theo mức độ', labelled(data.breakdowns.open.severity), 'Không có sự cố còn mở cuối kỳ.');
    } catch (error) {
        if (error.status === 401) throw error;
        byId('incidentTiles').replaceChildren();
        failed(['incidentStatusChart', 'incidentSeverityChart'], error.status === 403 ? error.message : 'Chưa tải được số liệu sự cố.');
        return error;
    }
}

async function load() {
    const params = query(byId('statisticsFilters'));
    const errors = (await Promise.all([loadAccounts(params), loadIncidents(params)])).filter(Boolean);
    // Cả hai nguồn lỗi thì để khung trang hiện nút thử lại; một nguồn lỗi vẫn giữ phần đã tải.
    if (errors.length === 2) throw errors[0];
    notice(errors.length ? 'Một số biểu đồ chưa tải được. Bạn có thể chọn Xem thống kê để thử lại.' : 'Đã cập nhật thống kê từ dữ liệu hệ thống.', errors.length > 0);
}

const page = createOperationsPage(load);
page.bind('statisticsFilters', 'submit', load);
bindChartTooltip();
page.start();
