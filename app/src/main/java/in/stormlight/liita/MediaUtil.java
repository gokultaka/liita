package in.stormlight.liita;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.Environment;

import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by ramo on 5/9/2015.
 */
public class MediaUtil {

    private static AtomicInteger counter = new AtomicInteger(1);


    /**
     * Configure the below constants
     */
    private static int CUSTOM_TEXT_BG_COLOR = Color.GRAY;
    private static int CUSTOM_TEXT_COLOR = Color.BLUE;




    /**
     *
     * @param images only supports JPEG images
     * @param audioFile only supports MP3 format
     * @param customText (optional)
     * @return mp4 video file
     */
    public static File getMP4VideoFile(File[] images, File audioFile, String customText) {

        if (images == null || images.length == 0) {
            return null;
        }


        File output = null;

        try {
            /**
             * First get the MP4 file from JPEG images
             */
            String outputFilename = getOutputFilename();
            output = getPathInMobileSD(outputFilename);

            SequenceEncoder encoder = new SequenceEncoder(output);

            int ti = images.length + 1;
            int td = ((int)(ti * 1.5)) + 1;

            List<Picture> pics = new ArrayList<>(ti);
            for (File image : images) {
                Bitmap bitmap = BitmapFactory.decodeFile(image.getPath());
                pics.add(convertBitmapToPicture(bitmap));
                bitmap.recycle();
            }

            /**
             * Append the custom text to the video file
             */

            Bitmap customBm = getCustomTextImage(customText);
            pics.add(convertBitmapToPicture(customBm));

            for (int i = 0; i < pics.size(); i++) {
                encoder.encodeNativeFrame(pics.get(i), (i*td), 1, td);
            }

            encoder.finish();


            /**
             * Now, we need to add audio to the video, need to
             * implement using MP4Parser
             */


            MovieCreator mc = new MovieCreator();
            Movie video = mc.build(Channels.newChannel(getResourceAsStream("/count-video.mp4")));
            Movie audio = mc.build(Channels.newChannel(getResourceAsStream("/count-english-audio.mp4")));


            List<Track> videoTracks = video.getTracks();
            video.setTracks(new LinkedList<Track>());

            List<Track> audioTracks = audio.getTracks();


            for (Track videoTrack : videoTracks) {
                video.addTrack(new AppendTrack(videoTrack, videoTrack));
            }
            for (Track audioTrack : audioTracks) {
                video.addTrack(new AppendTrack(audioTrack, audioTrack));
            }

            IsoFile out = new DefaultMp4Builder().build(video);
            FileOutputStream fos = new FileOutputStream(new File(String.format("output.mp4")));
            out.getBox(fos.getChannel());
            fos.close();





            //TODO


        }catch(Exception ex) {
            System.out.println("Exception occurred while processing : ");
            ex.printStackTrace();
        }

        return output;
    }


    private static Bitmap getCustomTextImage(String customText) {
        Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(CUSTOM_TEXT_BG_COLOR);

        Paint paint = new Paint();
        paint.setTextSize(40);
        paint.setTextScaleX(1.f);
        paint.setAlpha(0);
        paint.setAntiAlias(true);
        paint.setColor(CUSTOM_TEXT_BG_COLOR);
        canvas.drawText(customText, 30, 40, paint);

        return bitmap;
    }

    private static File getPathInMobileSD(String fileName) {
        File dir = Environment.getExternalStorageDirectory();
        String dirName = "Liita";
        return new File(dir.getPath() + File.separator + dirName+ File.separator+fileName);
    }

    private static Picture convertBitmapToPicture(Bitmap bitmap) {
        Picture pic = Picture.create((int)bitmap.getWidth(), (int)bitmap.getHeight(), ColorSpace.RGB);
        convertBitmap(bitmap, pic);
        return pic;
    }

    private static void convertBitmap(Bitmap src, Picture dst) {
        int[] dstData = dst.getPlaneData(0);
        int[] packed = new int[src.getWidth() * src.getHeight()];

        src.getPixels(packed, 0, src.getWidth(), 0, 0, src.getWidth(), src.getHeight());

        for (int i = 0, srcOff = 0, dstOff = 0; i < src.getHeight(); i++) {
            for (int j = 0; j < src.getWidth(); j++, srcOff++, dstOff += 3) {
                int rgb = packed[srcOff];
                dstData[dstOff]     = (rgb >> 16) & 0xff;
                dstData[dstOff + 1] = (rgb >> 8) & 0xff;
                dstData[dstOff + 2] = rgb & 0xff;
            }
        }
    }

    private static String getOutputFilename() {
        return "output_" + System.currentTimeMillis() + "_" +  counter.getAndIncrement() + ".mp4";
    }
}