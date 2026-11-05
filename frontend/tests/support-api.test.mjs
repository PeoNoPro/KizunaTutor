import {test,afterEach,mock} from 'node:test';
import assert from 'node:assert/strict';
import {fetchAPI,fetchMultipartAPI} from '../js/api.js';
afterEach(()=>mock.restoreAll());
test('ticket JSON writes get a ticket token rather than an admin-only token',async()=>{
 let count=0;mock.method(globalThis,'fetch',async(url,options)=>{count++;if(count===1){assert.ok(url.endsWith('/v1/tickets/csrf'));return Response.json({data:{headerName:'X-CSRF-TOKEN',token:'test-token'}});}assert.equal(options.headers['X-CSRF-TOKEN'],'test-token');return Response.json({data:{version:2}});});
 assert.deepEqual(await fetchAPI('/v1/tickets/test/claim',{method:'POST',body:{expectedVersion:1},csrfPath:'/v1/tickets/csrf'}),{version:2});
});
test('multipart preserves browser boundary and never fabricates success',async()=>{
 let count=0;mock.method(globalThis,'fetch',async(url,options)=>{count++;if(count===1)return Response.json({data:{headerName:'X-CSRF-TOKEN',token:'test-token'}});assert.ok(options.body instanceof FormData);assert.equal(options.headers['Content-Type'],undefined);assert.equal(options.credentials,'include');assert.equal(JSON.parse(await options.body.get('payload').text()).message,'Nội dung');return Response.json({message:'Tệp chưa hợp lệ.'},{status:400});});
 await assert.rejects(fetchMultipartAPI('/v1/tickets',{message:'Nội dung'},[]),error=>error.status===400&&error.message==='Tệp chưa hợp lệ.');
});
