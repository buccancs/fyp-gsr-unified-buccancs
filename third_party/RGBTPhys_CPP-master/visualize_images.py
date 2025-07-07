from matplotlib.figure import Figure
from matplotlib.backends.backend_agg import FigureCanvasAgg as FigureCanvas
import matplotlib.pyplot as plt

import os
import numpy as np
from datetime import datetime
import cv2
import argparse


def main(args_parser):

    data_dir = args_parser.path
    if not os.path.exists(data_dir):
        print("Incorrect path specified")
        return

    opt = args_parser.opt
    save_dir = ''
    if opt == 'record':
        save_dir = os.path.join(data_dir, 'out')
        if not os.path.exists(save_dir):
            os.makedirs(save_dir)

    fnames = os.listdir(data_dir)
    sorted_fnames = sorted(fnames)

    start_time = datetime.fromtimestamp(round(float(sorted_fnames[0].replace('.raw', '')) / 1000))
    end_time = datetime.fromtimestamp(round(float(sorted_fnames[-1].replace('.raw', '')) / 1000))
    total_secs = (end_time - start_time).total_seconds()
    fps = float(len(fnames)/total_secs) if total_secs != 0 else 0
    print("Start time:", start_time)
    print("End time:", end_time)
    print("Total seconds:", total_secs)
    print("Obtained FPS: ", fps)

    height = 512
    width = 640

    if opt == 'show':
        for cnt_fn in range(len(sorted_fnames)):
            fp = os.path.join(data_dir, sorted_fnames[cnt_fn])
            im = np.fromfile(fp, dtype='int16', sep="")
            im = im.reshape([height, width])
            im = (im * 0.04) - 273.15
            plt.imshow(im, cmap='magma')
            plt.axis('off')
            plt.show()

    elif opt == 'record':
        fig = Figure()
        ax = fig.subplots(1, 1)
        fig.tight_layout()
        # fig.set_figwidth(10)
        # fig.set_figheight(4)
        canvas = FigureCanvas(fig)

        for cnt_fn in range(len(sorted_fnames)):
            fp = os.path.join(data_dir, sorted_fnames[cnt_fn])
            im = np.fromfile(fp, dtype='int16', sep="")
            im = im.reshape([height, width])
            im = (im * 0.04) - 273.15
            ax.imshow(im, cmap='magma')
            ax.set_axis_off()
            canvas.draw()        
            canvas.print_jpg(os.path.join(save_dir, sorted_fnames[cnt_fn].replace('.raw', '.jpg')))
            ax.cla()

    else:
        print("Incorrect value specified for --opt")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--path', type=str, dest='path', help='Folder path having .raw thermal images')
    parser.add_argument('--opt', type=str, dest='opt', default='show', help='show (default) or record')
    parser.add_argument('REMAIN', nargs='*')
    args_parser = parser.parse_args()
    main(args_parser=args_parser)
