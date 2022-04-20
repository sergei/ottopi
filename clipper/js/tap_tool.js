console.log('Tap tool', JSON.stringify(source.selected.indices));

let top_title = 'Select First Point';
let bottom_title = 'Select Second Point';
let top_port_img = 'Top port image';
let top_stbd_img = 'Top starboard image';
let bot_port_img = 'Bottom port image';
let bot_stbd_img = 'Bottom starboard image';

if( source.selected.indices.length > 0){
    const idx = source.selected.indices[0];
    top_title = `%VMG: ${source.data['perc_vmg'][idx].toFixed(0)}% TWA: ${source.data['twa'][idx].toFixed(0)} SPD: ${source.data['sow'][idx].toFixed(1)} kts TWS: ${source.data['tws'][idx].toFixed(1)} kts VMG: ${source.data['vmg'][idx].toFixed(1)} kts `
    const port_img = encodeURI('file://' + source.data['port_image_name'][idx]);
    const stbd_img = encodeURI('file://' + source.data['stbd_image_name'][idx]);
    top_port_img = `<img src="${port_img}" height="300" style="transform:rotate(90deg);"/>`
    top_stbd_img = `<img src="${stbd_img}" height="300" style="transform:rotate(90deg);"/>`
}

if( source.selected.indices.length > 1){
    const idx = source.selected.indices[1];
    bottom_title = `%VMG: ${source.data['perc_vmg'][idx].toFixed(0)}% TWA: ${source.data['twa'][idx].toFixed(0)} SPD: ${source.data['sow'][idx].toFixed(1)} kts TWS: ${source.data['tws'][idx].toFixed(1)} kts VMG: ${source.data['vmg'][idx].toFixed(1)} kts `
    const port_img = encodeURI('file://' + source.data['port_image_name'][idx]);
    const stbd_img = encodeURI('file://' + source.data['stbd_image_name'][idx]);
    bot_port_img = `<img src="${port_img}" height="300px" style="transform:rotate(90deg);"/>`
    bot_stbd_img = `<img src="${stbd_img}" height="300px" style="transform:rotate(90deg);"/>`
}

div.text = `
<table>
   <tr>
        <td colspan="2">
            ${top_title}
        </td>
   </tr>
    <tr>
        <td style="height:600px">
            ${top_port_img}
        </td>
        <td style="height:600px">
            ${top_stbd_img}
        </td>
    </tr>
   <tr>
        <td colspan="2">
            ${bottom_title}
        </td>
   </tr>
    <tr>
        <td style="height:600px">
            ${bot_port_img}
        </td>
        <td style="height:600px">
            ${bot_stbd_img}
        </td>
    </tr>
</table>
`
