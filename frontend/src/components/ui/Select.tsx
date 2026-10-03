import React, { forwardRef, ReactNode } from 'react';
import { ChevronDown, Check } from 'lucide-react';
import { clsx } from 'clsx';

export interface SelectProps {
  value: string;
  onValueChange?: (value: any) => void;
  onChange?: (e: {target:{value:string}}) => void;
  children?: ReactNode;
  className?: string;
  label?: string;
  required?: boolean;
  disabled?: boolean;
}
interface TriggerProps { className?: string; children?: ReactNode; disabled?: boolean; }
interface ContentProps { className?: string; children?: ReactNode; }
interface ItemProps { value: string; children?: ReactNode; disabled?: boolean; onValueChange?: (v:string)=>void; isSelected?: boolean; }
interface ValueProps { placeholder?: string; }

const SelectRoot: React.FC<SelectProps> = ({ value, onValueChange, onChange, children, className, label, required, disabled }) => {
  const [open, setOpen] = React.useState(false);
  const rootRef = React.useRef<HTMLDivElement>(null);
  React.useEffect(() => {
    const close = (e: MouseEvent) => { if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false); };
    document.addEventListener('mousedown', close); return () => document.removeEventListener('mousedown', close);
  }, []);
  const items = React.Children.toArray(children).flatMap(child => {
    if (!React.isValidElement(child)) return [];
    if (child.type === SelectContent) return React.Children.toArray((child.props as ContentProps).children);
    return [];
  });
  return <div ref={rootRef} className={clsx('relative w-full', className)}>
    {label && <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-1.5">{label}{required ? ' *' : ''}</label>}
    <button type="button" disabled={disabled} aria-expanded={open} onClick={() => setOpen(v=>!v)} className="w-full flex items-center justify-between px-3 py-2.5 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg text-[var(--color-text-primary)] disabled:opacity-50 disabled:cursor-not-allowed">
      <span className={value ? '' : 'text-[var(--color-text-tertiary)]'}>{value || <SelectValue placeholder="Select..." />}</span><ChevronDown className="w-4 h-4" />
    </button>
    {open && <div className="absolute z-50 mt-1 w-full max-h-60 overflow-auto rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] shadow-lg py-1">{items.map((child, i) => React.isValidElement(child) ? React.cloneElement(child as React.ReactElement<ItemProps>, { key: child.key ?? i, onValueChange: v => { onValueChange?.(v); onChange?.({target:{value:v}}); setOpen(false); }, isSelected: (child.props as ItemProps).value === value }) : child)}</div>}
  </div>;
};
SelectRoot.displayName='Select';
export const SelectTrigger = forwardRef<HTMLSpanElement, TriggerProps>(({className, children}, ref)=><span ref={ref} className={className}>{children}</span>);
export const SelectValue = ({placeholder='Select...'}:ValueProps)=><span className="text-[var(--color-text-tertiary)]">{placeholder}</span>;
export const SelectContent = ({children}:ContentProps)=><>{children}</>;
export const SelectItem = forwardRef<HTMLDivElement, ItemProps>(({value,children,disabled,onValueChange,isSelected},ref)=><div ref={ref} role="option" aria-selected={isSelected} onClick={()=>!disabled&&onValueChange?.(value)} className={clsx('px-3 py-2 text-sm cursor-pointer flex justify-between', disabled&&'opacity-50 cursor-not-allowed', isSelected?'bg-[var(--color-primary)]/10 text-[var(--color-primary)]':'hover:bg-[var(--color-bg-hover)]')}><span>{children}</span>{isSelected&&<Check className="w-4 h-4"/>}</div>);
SelectItem.displayName='SelectItem';
export const Select = Object.assign(SelectRoot, {Trigger: SelectTrigger, Value: SelectValue, Content: SelectContent, Item: SelectItem});
