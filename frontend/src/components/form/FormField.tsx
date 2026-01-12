import type { ReactNode } from "react";

type Properties = {
    label: string;
    children: ReactNode;
    error?: string;
    helperText?: string;
};

export function FormField({ label, children, error, helperText }: Properties) {
    return (
        <div className="space-y-1.5">
            <label className="text-sm font-medium">{label}</label>
            {children}
            {error ? (
                <div className="text-xs text-red-600">{error}</div>
            ) : helperText ? (
                <div className="text-xs text-muted-foreground">{helperText}</div>
            ) : null}
        </div>
    );
}
