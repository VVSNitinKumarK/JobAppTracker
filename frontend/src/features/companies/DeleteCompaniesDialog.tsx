import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

type Properties = {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    companyNames: string[];
    isDeleting: boolean;
    onConfirm: () => void;
};

export function DeleteCompaniesDialog({
    open,
    onOpenChange,
    companyNames,
    isDeleting,
    onConfirm,
}: Properties) {
    return (
        <Dialog
            open={open}
            onOpenChange={(v) => !isDeleting && onOpenChange(v)}
        >
            <DialogContent className="sm:max-w-[520px]">
                <DialogHeader>
                    <DialogTitle>Delete companies?</DialogTitle>
                </DialogHeader>

                <div className="space-y-3">
                    <div className="text-sm text-muted-foreground">
                        This will permanently remove the selected companies.
                        Checklist will be removed automatically.
                    </div>

                    <div className="max-h-48 overflow-auto rounded-md border p-2">
                        <ul className="space-y-1 text-sm">
                            {companyNames.map((n) => (
                                <li key={n} className="truncate">
                                    • {n}
                                </li>
                            ))}
                        </ul>
                    </div>

                    <div className="flex items-center justify-end gap-2 pt-2">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={() => onOpenChange(false)}
                            disabled={isDeleting}
                        >
                            Cancel
                        </Button>

                        <Button
                            type="button"
                            variant="destructive"
                            onClick={onConfirm}
                            disabled={isDeleting || companyNames.length === 0}
                        >
                            {isDeleting ? "Deleting..." : "Delete"}
                        </Button>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
}
