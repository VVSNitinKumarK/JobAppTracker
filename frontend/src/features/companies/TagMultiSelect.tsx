import { useEffect, useMemo, useRef, useState } from "react";
import { X } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

export type TagOption = {
    key: string;
    label: string;
};

function normalize(s: string) {
    return s.trim().toLowerCase();
}

function uniqueByKey(list: TagOption[]) {
    const seen = new Set<string>();
    const out: TagOption[] = [];
    for (const tag of list) {
        if (seen.has(tag.key)) {
            continue;
        }

        seen.add(tag.key);
        out.push(tag);
    }

    return out;
}

type Properties = {
    label?: string;
    placeholder?: string;
    options: TagOption[];
    value: TagOption[];
    onChange: (next: TagOption[]) => void;
    disabled?: boolean;
};

export function TagMultiSelect({
    label = "Tags",
    placeholder = "Type to search (Enter to add)...",
    options,
    value,
    onChange,
    disabled,
}: Properties) {
    const [open, setOpen] = useState(false);
    const [text, setText] = useState("");

    const containerRef = useRef<HTMLDivElement | null>(null);
    const inputRef = useRef<HTMLInputElement | null>(null);

    const selectedKeys = useMemo(
        () => new Set(value.map((tag) => tag.key)),
        [value]
    );

    const filtered = useMemo(() => {
        const query = normalize(text);
        const base = options.filter((tag) => !selectedKeys.has(tag.key));
        if (!query) {
            return base.slice(0, 12);
        }

        return base
            .filter((tag) => normalize(tag.label).includes(query))
            .slice(0, 12);
    }, [options, selectedKeys, text]);

    useEffect(() => {
        function onDocMouseDown(event: MouseEvent) {
            if (!containerRef.current) {
                return;
            }

            if (!containerRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        }
        document.addEventListener("mousedown", onDocMouseDown);
        return () => document.removeEventListener("mousedown", onDocMouseDown);
    }, []);

    function addTag(tag: TagOption) {
        const next = uniqueByKey([...value, tag]);
        onChange(next);
        setText("");
        setOpen(true);
        inputRef.current?.focus();
    }

    function removeTag(tagKey: string) {
        onChange(value.filter((tag) => tag.key != tagKey));
    }

    function createFromText(raw: string): TagOption | null {
        const trimmed = raw.trim();
        if (!trimmed) {
            return null;
        }

        const match = options.find(
            (tag) => normalize(tag.label) === normalize(trimmed)
        );
        if (match) {
            return match;
        }

        const key = normalize(trimmed).replace(/\s+/g, "_");
        return { key, label: trimmed };
    }

    function onKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
        if (event.key === "Escape") {
            setOpen(false);
            return;
        }

        if (event.key === "Enter") {
            event.preventDefault();

            if (filtered.length > 0) {
                addTag(filtered[0]);
                return;
            }

            const created = createFromText(text);
            if (!created) {
                return;
            }

            if (!selectedKeys.has(created.key)) {
                addTag(created);
            } else {
                setText("");
            }
        }
    }

    return (
        <div className="space-y-1.5" ref={containerRef}>
            <label className="text-sm font-medium">{label}</label>

            {value.length > 0 ? (
                <div className="flex flex-wrap gap-1.5">
                    {value.map((tag) => (
                        <Badge
                            key={tag.key}
                            variant="secondary"
                            className="rounded-full flex items-center gap-1"
                        >
                            {tag.label}
                            <button
                                type="button"
                                aria-label={`Remove ${tag.label}`}
                                className={cn(
                                    "ml-1 rounded-sm p-0.5 hover:bg-muted",
                                    disabled && "pointer-events-none opacity-50"
                                )}
                                onClick={() => removeTag(tag.key)}
                                disabled={disabled}
                            >
                                <X className="h-3 w-3" />
                            </button>
                        </Badge>
                    ))}
                </div>
            ) : null}

            <div className="relative">
                <Input
                    ref={inputRef}
                    value={text}
                    onChange={(event) => setText(event.target.value)}
                    onFocus={() => setOpen(true)}
                    onKeyDown={onKeyDown}
                    placeholder={placeholder}
                    disabled={disabled}
                />

                {open ? (
                    <div className="absolute z-50 mt-1 w-full rounded-md border bg-background shadow-sm">
                        {filtered.length === 0 ? (
                            <div className="px-3 py-2 text-sm text-muted-foreground">
                                No matches. Press{" "}
                                <span className="font-medium">Enter</span> to
                                add "{text.trim() || "..."}".
                            </div>
                        ) : (
                            <ul className="max-h-48 overflow-auto py-1">
                                {filtered.map((tag) => (
                                    <li key={tag.key}>
                                        <button
                                            type="button"
                                            className="w-full px-3 py-2 text-left text-sm hover:bg-muted"
                                            onClick={() => addTag(tag)}
                                            disabled={disabled}
                                        >
                                            {tag.label}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                ) : null}
            </div>

            <div className="text-xs text-muted-foreground">
                Type to search. <span className="font-medium">Enter</span> adds.
            </div>
        </div>
    );
}
