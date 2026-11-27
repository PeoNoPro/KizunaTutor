import React, { ButtonHTMLAttributes, forwardRef } from 'react';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'primary-brand' | 'secondary' | 'retro' | 'accent-pink' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  fullWidth?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className = '', variant = 'primary', size = 'md', fullWidth = false, children, disabled, ...props }, ref) => {
    // Base styles
    const base = 'inline-flex items-center justify-center gap-1.5 font-semibold transition-all duration-150 select-none outline-none focus-visible:ring-2 focus-visible:ring-[#831843] focus-visible:ring-offset-1 disabled:opacity-50 disabled:pointer-events-none cursor-pointer';

    // Sizes
    const sizes = {
      sm: 'h-8 px-2.5 text-xs rounded-md',
      md: 'h-9 px-3.5 text-xs sm:text-sm rounded-lg',
      lg: 'h-11 px-5 text-sm sm:text-base rounded-lg'
    };

    // Variants (100% Shodo Design Tokens)
    const variants = {
      // Shodo Ink Button (Primary Black/Ink)
      primary: 'bg-[#1E293B] hover:bg-[#334155] text-white border border-[#0F172A] shadow-sm active:translate-x-[1px] active:translate-y-[1px]',
      
      // Brand Action Primary (#006194)
      'primary-brand': 'bg-[#006194] hover:bg-[#007BB9] text-white border border-[#004B73] shadow-sm active:translate-x-[1px] active:translate-y-[1px]',
      
      // Secondary Clean White
      secondary: 'bg-white hover:bg-slate-50 text-[#0F172A] border border-[#CBD5E1] shadow-sm active:bg-slate-100',
      
      // Neo-brutalist Retro Button (Signature Shodo style)
      retro: 'bg-white hover:bg-[#FAF8F2] text-[#0F172A] border-2 border-[#0F172A] shadow-[2px_2px_0px_0px_#0F172A] active:translate-x-[1px] active:translate-y-[1px] active:shadow-none',
      
      // Soft Berry / Pink Accent Button
      'accent-pink': 'bg-[#FFE5EC] hover:bg-[#FCE7F3] text-[#831843] border border-[#FBCFE8] shadow-xs active:bg-[#FBCFE8]',
      
      // Subtle Ghost Button
      ghost: 'bg-transparent hover:bg-slate-100 text-[#334155] border border-transparent',
      
      // Danger / Exit Action
      danger: 'bg-rose-50 hover:bg-rose-100 text-[#BA1A1A] border border-rose-200 active:bg-rose-200'
    };

    const width = fullWidth ? 'w-full' : '';

    return (
      <button
        ref={ref}
        disabled={disabled}
        className={`${base} ${sizes[size]} ${variants[variant]} ${width} ${className}`}
        {...props}
      >
        {children}
      </button>
    );
  }
);

Button.displayName = 'Button';
export default Button;