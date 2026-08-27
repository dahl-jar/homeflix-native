/**
 * Four-digit PIN entry model. `append` reports submit exactly once the
 * final digit lands, mirroring the web gate's auto-submit.
 */
export function createPinModel(length = 4) {
    const digits = [];

    return {
        digits,
        append(digit) {
            if (digits.length < length) digits.push(digit);
            const complete = digits.length === length;
            return { submit: complete, pin: complete ? digits.join('') : null };
        },
        clear() {
            digits.length = 0;
        }
    };
}
