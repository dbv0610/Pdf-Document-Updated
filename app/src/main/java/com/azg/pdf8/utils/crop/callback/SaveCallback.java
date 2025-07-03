package com.azg.pdf8.utils.crop.callback;

import android.net.Uri;

public interface SaveCallback extends Callback {
  void onSuccess(Uri uri);
}
