/** Share one in-flight aggregate request; counts never load ticket rows, messages or actions. */
export function createTicketCountsLoader(api) {
  const pending=new Map();
  return (filters=new URLSearchParams())=>{
    const query=new URLSearchParams();
    for(const name of ['category','priority','assignedTo','handover'])if(filters.get(name))query.set(name,filters.get(name));
    const key=query.toString();
    if(!pending.has(key)){
      const request=Promise.resolve().then(()=>api('/counts'+(key?'?'+key:''))).finally(()=>pending.delete(key));
      pending.set(key,request);
    }
    return pending.get(key);
  };
}
export const countTicketStatuses=(counts,statuses)=>statuses.reduce((sum,status)=>sum+Number(counts[status]||0),0);
