import moment from 'moment/moment'

export const formatDuration = (secs) => {
    const duration = moment.duration(secs,'seconds');
    let s;
    if ( secs > 3600  )
        s = duration.format("hh:mm:ss");
    else
        s = duration.format("mm:ss");

    return s;
};

export function toFixed(val, dec) {
    if (val === null) return '';
    if (typeof val !== 'undefined') {
        return val.toFixed(dec);
    }else{
        return '---';
    }
}

