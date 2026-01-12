import { useMemo } from "react";

import { useGetTags } from "./hooks";
import type { TagOption } from "./TagMultiSelect";

export function useTagOptions(): TagOption[] {
    const tagsQuery = useGetTags();

    return useMemo(() => {
        const raw = tagsQuery.data ?? [];
        return raw
            .map((tag) => ({
                key: tag.tagKey?.trim() || "",
                label: tag.tagName?.trim() || "",
            }))
            .filter(
                (tag): tag is TagOption =>
                    tag.key.length > 0 && tag.label.length > 0
            );
    }, [tagsQuery.data]);
}
