const {createHmac}=require('node:crypto');
const {writeFileSync,mkdirSync}=require('node:fs');
if(!/^jdbc:postgresql:\/\/127\.0\.0\.1:\d+\/[a-z0-9_]+_ticket_test$/.test(process.env.TICKET_TEST_DB_URL||''))throw new Error('Explicit isolated test database required');
const secret=process.env.TICKET_LOAD_SIGNING_KEY;if(!secret||secret.length<32)throw new Error('Set the isolated server signing key in TICKET_LOAD_SIGNING_KEY');
const encode=value=>Buffer.from(JSON.stringify(value)).toString('base64url');
const users=Array.from({length:100},(_,i)=>{const input=encode({alg:'HS256',typ:'JWT'})+'.'+encode({sub:`fe10-load-${i+1}@test.invalid`,exp:Math.floor(Date.now()/1000)+7200});return {role:'learner',token:input+'.'+createHmac('sha256',secret).update(input).digest('base64url')};});
mkdirSync('target',{recursive:true});writeFileSync('target/support-load-tokens.json',JSON.stringify(users),{mode:0o600});
console.log('Wrote isolated test tokens to ignored target/support-load-tokens.json');
