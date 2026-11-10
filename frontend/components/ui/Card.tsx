import React, { HTMLAttributes, forwardRef } from 'react';

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'brutalist' | 'warm-canvas' | 'tinted-pink' | 'amber-tip';
  hoverEffect?: boolean;
}

export const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ className = '', variant = 'default', hoverEffect = false, children, ...props }, ref) => {
    const base = 'transition-all duration-150 relative overflow-hidden';

    const variants = {
      // Standard Card: Clean white, subtle slate border, soft shadow
      default: 'bg-white border border-[#E2E8F0] rounded-xl p-4 shadow-sm text-[#334155]',
      
      // Neo-brutalist Retro Card (Shodo Signature): 2px ink border + 3px hard ink shadow
      brutalist: 'bg-white border-2 border-[#0F172A] rounded-xl p-4 sm:p-5 shadow-[3px_3px_0px_0px_#0F172A] text-[#0F172A]',
      
      // Warm Paper Canvas: #FAF8F2 with 2px ink border (for writing & Kanji boards)
      'warm-canvas': 'bg-[#FAF8F2] border-2 border-[#0F172A] rounded-lg p-3 sm:p-4 shadow-[2px_2px_0px_0px_#0F172A] text-[#0F172A]',
      
      // Tinted Soft Pink Card: Highlight/Summary cards
      'tinted-pink': 'bg-[#FFE5EC] border border-[#FBCFE8] rounded-xl p-4 text-[#831843]',
      
      // Amber Tip / Caution Card
      'amber-tip': 'bg-[#FEF3C7] border border-amber-200 rounded-xl p-3.5 sm:p-4 text-amber-900'
    };

    const hover = hoverEffect ? 'hover:border-[#0F172A] hover:shadow-[3px_3px_0px_0px_#0F172A] hover:-translate-y-0.5' : '';

    return (
      <div
        ref={ref}
        className={`${base} ${variants[variant]} ${hover} ${className}`}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';
export default Card;