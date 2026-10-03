import React from 'react';
export const Progress=({value=0,className=''}:{value?:number;className?:string})=><div className={`h-2 w-full overflow-hidden rounded-full bg-[var(--color-bg-tertiary)] ${className}`}><div className="h-full rounded-full bg-[var(--color-primary)] transition-all" style={{width:`${Math.max(0,Math.min(100,value))}%`}}/></div>;
