import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createTicketCountsLoader,countTicketStatuses} from '../js/pages/support-counts.js';

test('cards and status chips share one aggregate request without requesting ticket rows',async()=>{
  const paths=[];const load=createTicketCountsLoader(async path=>{paths.push(path);return {OPEN:2,IN_PROGRESS:3,ESCALATED:1};});
  const [cards,chips]=await Promise.all([load(),load(new URLSearchParams('status=OPEN&size=1'))]);
  assert.equal(cards,chips);assert.deepEqual(paths,['/counts']);
  assert.equal(countTicketStatuses(cards,['OPEN','IN_PROGRESS','ESCALATED']),6);
  assert.equal(countTicketStatuses(cards,['RESOLVED']),0);
});
test('different filters stay separate and later refreshes do not reuse stale counts',async()=>{
  const paths=[];const load=createTicketCountsLoader(async path=>{paths.push(path);return {OPEN:paths.length};});
  await Promise.all([load(),load(new URLSearchParams('category=BOOKING&priority=HIGH'))]);
  assert.equal((await load()).OPEN,3);
  assert.deepEqual(paths,['/counts','/counts?category=BOOKING&priority=HIGH','/counts']);
});
test('failed aggregate requests can be retried and do not fabricate zero counts',async()=>{
  let calls=0;const load=createTicketCountsLoader(async()=>{if(!calls++)throw new Error('DB unavailable');return {OPEN:1};});
  await assert.rejects(load(),/DB unavailable/);assert.equal((await load()).OPEN,1);
});
