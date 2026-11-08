import React, { HTMLAttributes, forwardRef } from 'react';

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: 'pink' | 'amber' | 'green' | 'ink' | 'slate' | 'danger' | 'info';
  shape?: 'pill' | 'rounded';
}

export const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className = '', variant = 'pink', shape = 'pill', children, ...props }, ref) => {
    const base = 'inline-flex items-center gap-1 font-semibold select-none';

    const shapes = {
      pill: 'rounded-full px-2.5 py-0.5 text-xs',
      rounded: 'rounded-sm px-2 py-0.5 text-[11px]'
    };

    const variants = {
      // Shodo Berry / Pink Badge
      pink: 'bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]',
      
      // Amber Warning / JLPT Alert
      amber: 'bg-[#FEF3C7] text-[#D97706] border border-amber-200',
      
      // Success Green
      green: 'bg-emerald-50 text-[#059669] border border-emerald-200',
      
      // Solid Ink Badge
      ink: 'bg-[#0F172A] text-white border border-[#0F172A]',
      
      // Subtle Slate / Metadata
      slate: 'bg-slate-100 text-[#475569] border border-[#E2E8F0]',
      
      // Danger / Error
      danger: 'bg-rose-50 text-[#BA1A1A] border border-rose-200',
      
      // Info Blue
      info: 'bg-sky-50 text-[#0284C7] border border-sky-200'
    };

    return (
      <span
        ref={ref}
        className={`${base} ${shapes[shape]} ${variants[variant]} ${className}`}
        {...props}
      >
        {children}
      </span>
    );
  }
);

Badge.displayName = 'Badge';
export default Badge;