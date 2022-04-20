import argparse
import os

import numpy as np
import pandas as pd
from bokeh.models import RangeSlider, CustomJS, CheckboxGroup, HoverTool, Div, ColumnDataSource
from bokeh.models import CDSView, CustomJSFilter
from bokeh.plotting import figure, show
from bokeh.layouts import layout


def perf_view(args):
    date_cols = ['utc']
    port_images = pd.read_csv(os.path.join(args.camera_csv_dir, 'port.csv', ), parse_dates=date_cols)
    stbd_images = pd.read_csv(os.path.join(args.camera_csv_dir, 'stbd.csv'), parse_dates=date_cols)
    boat_data = pd.read_csv(os.path.join(args.camera_csv_dir, 'targets.csv'), parse_dates=date_cols)

    all_images = pd.merge_asof(port_images, stbd_images, on='utc')
    data = pd.merge_asof(boat_data, all_images, on='utc')

    vmg = np.abs(data['sow'] * np.cos(np.radians(data['twa'])))
    target_vmg = np.abs(data['target_sow'] * np.cos(np.radians(data['target_twa'])))

    perc_vmg = vmg / target_vmg * 100

    perc_sow = data['sow'] / data['target_sow'] * 100

    arr_hist, edges = np.histogram(data['tws'])
    # Put the information in a dataframe
    tws = pd.DataFrame({'tws': arr_hist, 'left': edges[:-1], 'right': edges[1:]})

    # Create the blank plot
    p1 = figure(plot_height=400, plot_width=400,
                title='Histogram of TWS',
                x_axis_label='TWS',
                y_axis_label='Samples num')

    # Add a quad glyph
    p1.quad(bottom=0, top=tws['tws'],
            left=tws['left'], right=tws['right'],
            fill_color='red', line_color='black')

    ht = HoverTool()

    p2 = figure(plot_height=400, plot_width=400,
                title='Percent of target speed',
                x_axis_label='UTC',
                y_axis_label='Percent', tools=['box_select', 'reset', 'zoom_in', 'zoom_out', 'box_zoom', "tap", ht])

    source = ColumnDataSource(data={'utc': data['utc'], 'sow': perc_vmg, 'tws': data['tws'], 'twa': data['twa'],
                                    'port_image_name': data['port_image_name'],
                                    'stbd_image_name': data['stbd_image_name'],
                                    })

    # p2.circle(targets['utc'], perc_sow, size=2, color="red", alpha=0.8)
    # p2.square(targets['utc'], perc_vmg, size=4, color="yellow", alpha=0.8)
    # p2.square(targets['utc'], targets['twa'], size=1, color="green", alpha=0.8)

    min_tws = data['tws'].min()
    max_tws = data['tws'].max()
    slider = RangeSlider(start=min_tws, end=max_tws, value=(min_tws, max_tws), step=.1, title="Exp")
    callback = CustomJS(args=dict(s=source), code="""
        s.change.emit();
    """)
    # slider.js_on_change('value_throttled', callback)
    slider.js_on_change('value', callback)

    tack = CheckboxGroup(labels=['port', 'starboard'], active=[1])
    tack.js_on_click(callback)

    up_down = CheckboxGroup(labels=['Upwind', 'Downwind'], active=[0])
    up_down.js_on_click(callback)

    filt = CustomJSFilter(args=dict(slider=slider, tack=tack, up_down=up_down), code="""
            var indices = new Array(source.get_length());
            var start = slider.value[0];
            var end = slider.value[1];
            var boat_tack = tack.active; 
            var ud = up_down.active;
            
            for (var i=0; i < source.get_length(); i++){
            
                if (source.data['tws'][i] >= start && source.data['tws'][i] <= end){
                    indices[i] = true;
                    
                    var twa = source.data['twa'][i];
                    // Starboard tack is not selected
                    if (twa >= 0 && !boat_tack.includes(1)) {
                        indices[i] = false;
                    }
                    
                    // Port tack is not selected
                    if (twa < 0 && !boat_tack.includes(0)) {
                        indices[i] = false;
                    }                    
                    
                    // Upwind is not selected 
                    if (Math.abs(twa) < 90 && !ud.includes(0)) {
                        indices[i] = false;
                    }                    
                    
                    // Downwind is not selected 
                    if (Math.abs(twa) >= 90 && !ud.includes(1)) {
                        indices[i] = false;
                    }                    
                    
                } else {
                    indices[i] = false;
                }
            }
            return indices;
            """)

    div = Div(text="Hello")

    ht.callback = CustomJS(args=dict(div=div, source=source), code="""
        const hit_test_result = cb_data.index;
        const indices = hit_test_result.indices;
        if (indices.length > 0) {
            const idx = indices[0];
            const port_img = encodeURI('file://' + source.data['port_image_name'][idx]);
            const stbd_img = encodeURI('file://' + source.data['stbd_image_name'][idx]);
            
            console.log('idx=', indices[0], 'port_img=', port_img );
            div.text = `
            <table>
                <tr>
                    <td>
                        <img src="${port_img}" height="300" style="transform:rotate(90deg);"/>
                    </td>
                    <td>
                        <img src="${stbd_img}" height="300" style="transform:rotate(90deg);"/>
                    </td>
                </tr>
            </table>
            `;
        }
    """)

    view = CDSView(source=source, filters=[filt])
    p2.circle(x='utc', y='sow', source=source, view=view)
    p2.square(x='utc', y='twa', source=source, view=view, color='green')

    # noinspection PyTypeChecker
    show(layout([
        [slider, tack, up_down],
        [p1, ],
        [p2, div]
    ]))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(fromfile_prefix_chars='@')
    parser.add_argument("--cfg-file", help="Config YAML file", default='cfg/config.yaml')
    parser.add_argument("--work-dir", help="Working directory", default='/tmp')
    parser.add_argument("--ignore-cache", help="Use cached data only", default=False, action='store_true')
    parser.add_argument("--camera-csv-dir", help="GoPro SD card directory", required=True)
    parser.add_argument("--sk-file", help="SignalK file", required=False)
    parser.add_argument("--sk-zip", help="Zip file containing SignalK logs", required=False)
    perf_view(parser.parse_args())
