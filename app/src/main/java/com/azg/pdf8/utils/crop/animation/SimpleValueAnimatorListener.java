package com.azg.pdf8.utils.crop.animation;

public interface SimpleValueAnimatorListener {
  void onAnimationStarted();

  void onAnimationUpdated(float scale);

  void onAnimationFinished();
}
