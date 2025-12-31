import { useEffect, useRef, useState } from "react";

export function useElementWidth<T extends HTMLElement>() {
    const ref = useRef<T | null>(null);
    const [width, setWidth] = useState(0);

    useEffect(() => {
        const el = ref.current;
        if (!el) return;

        const ro = new ResizeObserver((entries) => {
            const w = entries[0]?.contentRect?.width ?? 0;
            setWidth(w);
        });

        ro.observe(el);
        setWidth(el.getBoundingClientRect().width);

        return () => ro.disconnect();
    }, []);

    return { ref, width };
}
