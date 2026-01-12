import { useEffect, useRef } from "react";

type Properties = {
    checked: boolean;
    indeterminate: boolean;
    onChange: () => void;
    ariaLabel: string;
    className?: string;
    disabled?: boolean;
};

export function IndeterminateCheckbox(properties: Properties) {
    const ref = useRef<HTMLInputElement | null>(null);

    useEffect(() => {
        if (!ref.current) {
            return;
        }

        ref.current.indeterminate = properties.indeterminate;
    }, [properties.indeterminate]);

    return (
        <input
            ref={ref}
            type="checkbox"
            aria-label={properties.ariaLabel}
            checked={properties.checked}
            onChange={properties.onChange}
            disabled={properties.disabled}
            className={
                properties.className ??
                "h-4 w-4 rounded border border-input bg-background align-middle"
            }
        />
    );
}
