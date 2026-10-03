import React, { forwardRef, ReactNode, createContext, useContext } from 'react';
import { X } from 'lucide-react';
import { clsx } from 'clsx';
import { createPortal } from 'react-dom';

interface DialogProps { open: boolean; onOpenChange: (open: boolean) => void; children: ReactNode; }
interface PartProps { className?: string; children?: ReactNode; }
interface TriggerProps { asChild?: boolean; children: React.ReactElement; }
interface DialogContextValue { open: boolean; onOpenChange: (open: boolean) => void; }
const DialogContext = createContext<DialogContextValue | null>(null);

const DialogRoot = ({ open, onOpenChange, children }: DialogProps) => {
  React.useEffect(() => {
    if (!open) return;
    const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') onOpenChange(false); };
    document.addEventListener('keydown', onKey);
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => { document.removeEventListener('keydown', onKey); document.body.style.overflow = previous; };
  }, [open, onOpenChange]);

  return (
    <DialogContext.Provider value={{ open, onOpenChange }}>
      {open ? createPortal(
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => onOpenChange(false)} />
          <div role="dialog" aria-modal="true" className="relative z-10 w-full max-w-lg rounded-2xl bg-[var(--color-surface)] border border-[var(--color-border)] shadow-2xl">
            <button type="button" aria-label="Close" onClick={() => onOpenChange(false)} className="absolute right-3 top-3 z-10 p-2 rounded-full hover:bg-[var(--color-bg-hover)] text-[var(--color-text-secondary)]">
              <X className="w-4 h-4" />
            </button>
            {children}
          </div>
        </div>, document.body
      ) : null}
    </DialogContext.Provider>
  );
};

const Trigger = ({ children }: TriggerProps) => {
  const context = useContext(DialogContext);
  if (!context) return children;
  return React.cloneElement(children, {
    onClick: (event: React.MouseEvent) => {
      children.props.onClick?.(event);
      if (!event.defaultPrevented) context.onOpenChange(true);
    },
  });
};

const Content = forwardRef<HTMLDivElement, PartProps>(({ className, children }, ref) => <div ref={ref} className={clsx('p-6', className)}>{children}</div>);
const Header = ({ className, children }: PartProps) => <div className={clsx('mb-4 pr-8', className)}>{children}</div>;
const Title = ({ className, children }: PartProps) => <h2 className={clsx('text-xl font-semibold text-[var(--color-text-primary)]', className)}>{children}</h2>;
const Description = ({ className, children }: PartProps) => <p className={clsx('text-sm text-[var(--color-text-secondary)] mt-1', className)}>{children}</p>;
const Footer = ({ className, children }: PartProps) => <div className={clsx('flex items-center justify-end gap-2 mt-6 pt-4 border-t border-[var(--color-border)]', className)}>{children}</div>;

export const Dialog = Object.assign(DialogRoot, { Trigger, Content, Header, Title, Description, Footer, displayName: 'Dialog' });
export { Trigger as DialogTrigger, Content as DialogContent, Header as DialogHeader, Title as DialogTitle, Description as DialogDescription, Footer as DialogFooter };
