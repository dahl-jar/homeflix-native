export const PICKER_OPTION_HEIGHT = 45;

export function pickerInsetForViewport(viewportHeight) {
    return Math.max(0, viewportHeight / 2 - PICKER_OPTION_HEIGHT / 2);
}

export function pickerOffsetForIndex(index) {
    return Math.max(0, index) * PICKER_OPTION_HEIGHT;
}

export function pickerIndexFromOffset(offset, entryCount) {
    if (entryCount <= 0) return -1;
    const index = Math.round(offset / PICKER_OPTION_HEIGHT);
    return Math.max(0, Math.min(entryCount - 1, index));
}

export function selectedPickerIndex(entries, isSelected) {
    if (entries.length === 0) return -1;
    return Math.max(0, entries.findIndex((entry) => isSelected(entry)));
}
