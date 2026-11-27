import React, { ReactNode } from 'react';

export interface PageHeaderProps {
  title: string;
  subtitle?: string;
  badgeText?: string;
  breadcrumbs?: Array<{ label: string; href?: string }>;
  actions?: ReactNode;
}

export interface PageContainerProps {
  maxWidth?: '6xl' | '7xl' | 'full';
  header?: PageHeaderProps;
  children: ReactNode;
  className?: string;
}

export const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  subtitle,
  badgeText,
  breadcrumbs,
  actions
}) => {
  return (
    <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pb-3 border-b border-[#E2E8F0]">
      <div>
        {breadcrumbs && breadcrumbs.length > 0 && (
          <nav className="flex items-center gap-1.5 text-xs text-[#64748B] mb-1.5">
            {breadcrumbs.map((crumb, idx) => (
              <React.Fragment key={idx}>
                {crumb.href ? (
                  <a href={crumb.href} className="hover:text-[#0F172A] hover:underline">
                    {crumb.label}
                  </a>
                ) : (
                  <span className="text-[#0F172A] font-semibold">{crumb.label}</span>
                )}
                {idx < breadcrumbs.length - 1 && <span className="text-slate-300">/</span>}
              </React.Fragment>
            ))}
          </nav>
        )}

        <div className="flex items-center gap-2">
          <h1 className="text-xl sm:text-2xl font-extrabold text-[#0F172A] tracking-tight">
            {title}
          </h1>
          {badgeText && (
            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]">
              {badgeText}
            </span>
          )}
        </div>

        {subtitle && (
          <p className="text-xs sm:text-sm text-[#64748B] mt-1 leading-normal">
            {subtitle}
          </p>
        )}
      </div>

      {actions && (
        <div className="flex items-center gap-2 shrink-0">
          {actions}
        </div>
      )}
    </header>
  );
};

export const PageContainer: React.FC<PageContainerProps> = ({
  maxWidth = '6xl',
  header,
  children,
  className = ''
}) => {
  const maxW = {
    '6xl': 'max-w-6xl',
    '7xl': 'max-w-7xl',
    'full': 'max-w-full'
  };

  return (
    <main className={`flex-1 overflow-y-auto w-full ${maxW[maxWidth]} mx-auto p-3 sm:p-4 md:p-6 space-y-4 ${className}`}>
      {header && <PageHeader {...header} />}
      {children}
    </main>
  );
};

export default PageContainer;