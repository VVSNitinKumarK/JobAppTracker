import { Toaster as Sonner, type ToasterProps } from "sonner";
import React from "react";

function Toaster({ ...props }: ToasterProps) {
    return (
        <Sonner
            className="toaster group"
            style={
                {
                    "--normal-bg": "hsl(var(--background))",
                    "--normal-text": "hsl(var(--foreground))",
                    "--normal-border": "hsl(var(--border))",
                    "--success-bg": "hsl(var(--background))",
                    "--success-text": "hsl(var(--foreground))",
                    "--success-border": "hsl(142.1 76.2% 36.3%)",
                    "--error-bg": "hsl(var(--background))",
                    "--error-text": "hsl(var(--foreground))",
                    "--error-border": "hsl(var(--destructive))",
                } as React.CSSProperties
            }
            toastOptions={{
                classNames: {
                    toast: "group toast group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:border-border group-[.toaster]:shadow-lg",
                    description: "group-[.toast]:text-muted-foreground",
                    actionButton:
                        "group-[.toast]:bg-primary group-[.toast]:text-primary-foreground",
                    cancelButton:
                        "group-[.toast]:bg-muted group-[.toast]:text-muted-foreground",
                    success:
                        "group-[.toaster]:!bg-emerald-50 group-[.toaster]:!border-emerald-500 group-[.toaster]:!text-emerald-900 dark:group-[.toaster]:!bg-emerald-950 dark:group-[.toaster]:!text-emerald-100",
                    error: "group-[.toaster]:!bg-red-50 group-[.toaster]:!border-red-500 group-[.toaster]:!text-red-900 dark:group-[.toaster]:!bg-red-950 dark:group-[.toaster]:!text-red-100",
                },
            }}
            {...props}
        />
    );
}

export { Toaster };
