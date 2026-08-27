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
