import {test, afterEach, mock} from 'node:test';
import assert from 'node:assert/strict';
import {fetchAPI} from '../js/api.js';
import {API_CONFIG} from '../js/config/api.config.js';
afterEach(() => mock.restoreAll());
test('read uses central configuration and cookie credentials', async () => {
    mock.method(globalThis, 'fetch', async (url, options) => {
        assert.equal(url, API_CONFIG.BASE_URL + '/policies'); assert.equal(options.credentials, 'include');
        return Response.json({success: true, data: []});
    });
    assert.deepEqual(await fetchAPI('/policies'), []);
});
test('writes obtain a server token then send it with the session', async () => {
    let calls = 0;
    mock.method(globalThis, 'fetch', async (url, options) => {
        calls++;
        if (calls === 1) {assert.equal(url, API_CONFIG.BASE_URL + '/admin/operations/csrf');return Response.json({success: true, data: {token: 'test-only-token', headerName: 'X-CSRF-TOKEN'}});}
        assert.equal(options.headers['X-CSRF-TOKEN'], 'test-only-token'); assert.equal(options.method, 'PUT');
        assert.equal(options.credentials, 'include'); return Response.json({success: true, data: {version: 1}});
    });
    assert.deepEqual(await fetchAPI('/admin/operations/booking-policy', {method: 'PUT', body: {}}), {version: 1});
    assert.equal(calls, 2);
});
test('network and provider failures never become success or fabricated zero data', async () => {
    mock.method(globalThis, 'fetch', async () => {throw new TypeError('network test fixture');});
    await assert.rejects(fetchAPI('/policies'), /Chưa kết nối/);
});
test('unauthorized response and field errors remain human readable', async () => {
    mock.method(globalThis, 'fetch', async () => Response.json({success: false, message: 'Vui lòng đăng nhập.'}, {status: 401}));
    await assert.rejects(fetchAPI('/policies'), error => error.status === 401 && error.message === 'Vui lòng đăng nhập.');
});
