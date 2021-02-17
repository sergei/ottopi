export function toFixed(val, dec) {
    if (val === null) return '';
    if (typeof val !== 'undefined') {
        return val.toFixed(dec);
    }else{
        return '---';
    }
}

