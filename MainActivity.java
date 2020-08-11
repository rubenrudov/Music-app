package il.co.musicplayer;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public int position = 0;
    public boolean isStart = true;
    ImageButton playOrStop, next, prev;
    TextView time, durationText;
    ListView songList;
    SeekBar duration;
    private List<String> musicFilesLst;
    private static final int REQUEST = 12345;
    private boolean isMusicPlayer;
    public MediaPlayer mediaPlayer = new MediaPlayer();
    @SuppressLint("NewApi")
    private boolean isAllowed() {
        for (int i = 0; i < count; i++) {
            if (checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    private static final String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int count = 1; // Counts the permissions

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playOrStop = findViewById(R.id.PlayOrStop);
        mediaPlayer.stop();
        playOrStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == playOrStop) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        playOrStop.setImageResource(R.drawable.ic_play);
                    } else {
                        position = mediaPlayer.getCurrentPosition();
                        if (mediaPlayer != null && !isStart) {
                            mediaPlayer.start();
                        } else if (isStart) {
                            try {
                                play(position);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        isStart = false;
                        playOrStop.setImageResource(R.drawable.ic_pause);
                    }
                }
            }
        });
        next = findViewById(R.id.NextSong);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position < musicFilesLst.size() - 1) {
                    position++;
                } else {
                    position = 0;
                }
                try {
                    play(position);
                    playOrStop.setImageResource(R.drawable.ic_pause);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        prev = findViewById(R.id.PrevSong);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position > 0) {
                    position--;
                } else {
                    position = musicFilesLst.size() - 1;
                }
                try {
                    play(position);
                    playOrStop.setImageResource(R.drawable.ic_pause);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        songList = findViewById(R.id.SongListView);
        time = findViewById(R.id.TimePassed);
        time.setText("0:0");
        durationText = findViewById(R.id.DurationText);
        duration = findViewById(R.id.Duration);
        duration.setProgress(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isAllowed()) {
            requestPermissions(permissions, REQUEST); // Passes the permissions array and an integer(1)
            return;
        }
        if (!isMusicPlayer) {
            musicFilesLst = new ArrayList<>();
            final MusicAdapter musicAdapter = new MusicAdapter();
            fillList();
            musicAdapter.setMusicFiles(musicFilesLst);
            duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if(fromUser) {
                        mediaPlayer.seekTo(progress);
                        duration.setProgress(progress);
                        String digs = progress/ 1000 % 60 >= 10 ? String.valueOf(progress/ 1000 % 60) : "0" + progress / 1000 % 60;
                        time.setText(progress / 60000 + ":" + digs);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            songList.setAdapter(musicAdapter);
            songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    try {
                        time.setText("0:0");
                        duration.setProgress(0);
                        play(position);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            isMusicPlayer = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        /*
       If the permission allowed resuming the activity else clears user data, closes the app
       when the user reopens the app the permission request appears once more
       until the user allows to get files from the external storage
        */
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (isAllowed()) {
            onResume();
        } else {
            ((ActivityManager) (Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE)))).clearApplicationUserData();
            recreate();
        }
    }

    private void fillList() {
        musicFilesLst.clear();
        addNewFile(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)));
        addNewFile(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
    }

    private void addNewFile(String path) {
        File directory = new File(path);
        if (directory.exists()) {
            File[] files = directory.listFiles();
            assert files != null; // NoNullException handling
            for (File file : files) {
                String absPath = file.getAbsolutePath();
                if (absPath.endsWith(".mp3"))
                    musicFilesLst.add(absPath);
            }
        } else {
            directory.mkdir();
        }
    }

    @SuppressLint("SetTextI18n")
    public void play(int position) throws IOException {
        if (musicFilesLst != null && mediaPlayer.isPlaying())
            mediaPlayer.reset();
        assert musicFilesLst != null;
        String path = musicFilesLst.get(position);
        TextView songName = findViewById(R.id.SongName);
        songName.setText(path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.')));
        mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(musicFilesLst.get(position)));
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // duration.setMax(mediaPlayer.getDuration());
                duration.setMax(mediaPlayer.getDuration());
                mediaPlayer.start();
                playOrStop.setImageResource(R.drawable.ic_pause);
            }
        });
        durationSetting(mediaPlayer.getDuration());
        playOrStop.setImageResource(R.drawable.ic_pause);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                next.callOnClick();
                playOrStop.setImageResource(R.drawable.ic_pause);
            }
        });
        if(mediaPlayer != null)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mediaPlayer != null) {
                        try {
//                        Log.i("Thread ", "Thread Called");
                            // create new message to send to handler
                            if (mediaPlayer.isPlaying()) {
                                Message msg = new Message();
                                msg.what = mediaPlayer.getCurrentPosition();
                                handler.sendMessage(msg);
                                Thread.sleep(1000);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int current_position = msg.what;
            duration.setProgress(current_position);
            String cTime = setCurrentTime(current_position);
            time.setText(cTime);
        }
    };
    @SuppressLint("SetTextI18n")
    void durationSetting(int d) {
        int min = d / 1000 / 60;
        int sec = d / 1000 % 60;
        durationText.setText(min + ":" + sec);
        duration.setMax(d / 1000);
    }

    String setCurrentTime(int progress)
    {
        String min = String.valueOf(progress / 1000 / 60);
        String sec = progress / 1000 % 60 > 10 ? String.valueOf(progress / 1000 % 60) : "0" +  String.valueOf(progress / 1000 % 60);
        return min + ":" + sec;
    }

    static class MusicAdapter extends BaseAdapter {
        private List<String> musicFiles = new ArrayList<>();

        @Override
        public int getCount() {
            return this.musicFiles.size();
        }

        void setMusicFiles(List<String> newMusic) {
            this.musicFiles.clear();
            this.musicFiles.addAll(newMusic);
            this.notifyDataSetChanged();
        }

        @Override
        public String getItem(int position) {
            return null; // Temp return .. will be changed later
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.song, parent, false);
            convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.songLabel)));
            ViewHolder holder = (ViewHolder) convertView.getTag();
            String song = musicFiles.get(position);
            holder.information.setText(song.substring(song.lastIndexOf('/') + 1, song.lastIndexOf('.')));
            return convertView;
        }

        static class ViewHolder {
            TextView information;

            ViewHolder(TextView songInformation) {
                this.information = songInformation;
            }
        }
    }
}
