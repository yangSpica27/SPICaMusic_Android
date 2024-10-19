package me.spica27.spicamusic.processer.fourier;

public class ComplexNumberArray {
  private float[] realArray;
  private float[] imaginaryArray;
  private int size;

  public ComplexNumberArray(int size) {
    if (size <= 0) {
      this.size = 128;
      realArray = new float[this.size];
      imaginaryArray = new float[this.size];
    } else {
      this.size = size;
      realArray = new float[size];
      imaginaryArray = new float[size];
    }
  }

  //设置复数
  public void setComplexNumber(int position, float realPart, float imaginaryPart) {
    realArray[position] = realPart;
    imaginaryArray[position] = imaginaryPart;
  }

  //设置复数
  public void setComplexNumber(int position, ComplexNumber cNum) {
    if (cNum != null) {
      realArray[position] = cNum.getRealPart();
      imaginaryArray[position] = cNum.getImaginaryPart();
    }
  }

  //设置全部复数
  public void setAllComplexNumber(ComplexNumber[] cNumArray) {
    if (cNumArray != null) {
      for (int i = 0; i < size; i++) {
        realArray[i] = cNumArray[i].getRealPart();
        imaginaryArray[i] = cNumArray[i].getImaginaryPart();
      }
    }
  }

  //设置全部复数
  public void setAllComplexNumber(float[] realPartArray, float[] imaginaryPartArray) {
    if (realPartArray != null) {
      for (int i = 0; i < size; i++) {realArray[i] = realPartArray[i];}
    }

    if (imaginaryPartArray != null) {
      for (int i = 0; i < size; i++) {imaginaryArray[i] = imaginaryPartArray[i];}
    }
  }

  //清除
  public void clear(int position) {
    realArray[position] = 0;
    imaginaryArray[position] = 0;
  }

  //全部清除
  public void clearAll() {
    for (int i = 0; i < size; i++) {clear(i);}
  }

  //获取复数
  public ComplexNumber getComplexNumber(int position) {
    return new ComplexNumber(realArray[position], imaginaryArray[position]);
  }

  //获取实部
  public float getRealPart(int position) {
    return realArray[position];
  }

  //获取所有的实部
  public float[] getAllRealPart() {
    return realArray;
  }

  //获取虚部
  public float getImaginaryPart(int position) {
    return imaginaryArray[position];
  }

  //获取所有的虚部
  public float[] getAllImaginaryPart() {
    return imaginaryArray;
  }

  //设置实部
  public void setRealPart(int position, float realPart) {
    realArray[position] = realPart;
  }

  //设置虚部
  public void setImaginaryPart(int position, float imaginaryPart) {
    imaginaryArray[position] = imaginaryPart;
  }

  //获取幅值
  public float getAmplitude(int position) {
    return (float) Math.sqrt(
        Math.pow(realArray[position], 2) + Math.pow(imaginaryArray[position], 2));
  }

  //获取所有幅值
  public float[] getAllAmplitude() {
    float[] amplitudeArray = new float[size];
    for (int i = 0; i < size; i++) {amplitudeArray[i] = getAmplitude(i);}
    return amplitudeArray;
  }

  //获取相位
  public float getPhase(int position) {
    float realPart = realArray[position];
    float imaginaryPart = imaginaryArray[position];
    if (realPart == 0) {
      if (imaginaryPart == 0) {return 0;} else if (imaginaryPart > 0) {
        return (float) (Math.PI / 2);
      } else {
        return (float) (-Math.PI / 2);
      }
    } else {
      return (float) Math.atan(imaginaryPart / realPart);
    }
  }

  //获取所有相位
  public float[] getAllPhase() {
    float[] phaseArray = new float[size];
    for (int i = 0; i < size; i++) {phaseArray[i] = getPhase(i);}
    return phaseArray;
  }

  //加法
  public void add(int position, ComplexNumber addend) {
    if (addend != null) {
      realArray[position] += addend.getRealPart();
      imaginaryArray[position] += addend.getImaginaryPart();
    }
  }

  //全部加法
  public void addAll(ComplexNumber addend) {
    if (addend != null) {
      for (int i = 0; i < size; i++) {add(i, addend);}
    }
  }

  //减法
  public void subtract(int position, ComplexNumber subtrahend) {
    if (subtrahend != null) {
      realArray[position] -= subtrahend.getRealPart();
      imaginaryArray[position] -= subtrahend.getImaginaryPart();
    }
  }

  //全部减法
  public void subtractAll(ComplexNumber subtrahend) {
    if (subtrahend != null) {
      for (int i = 0; i < size; i++) {subtract(i, subtrahend);}
    }
  }

  //乘法
  public void multiply(int position, ComplexNumber multiplier) {
    if (multiplier != null) {
      float newRealPart = realArray[position] * multiplier.getRealPart() -
          imaginaryArray[position] * multiplier.getImaginaryPart();
      float newImaginaryPart = realArray[position] * multiplier.getImaginaryPart() +
          imaginaryArray[position] * multiplier.getRealPart();
      realArray[position] = newRealPart;
      imaginaryArray[position] = newImaginaryPart;
    }
  }

  //全部乘法
  public void multiplyAll(ComplexNumber multiplier) {
    if (multiplier != null) {
      for (int i = 0; i < size; i++) {multiply(i, multiplier);}
    }
  }

  //乘法
  public void multiply(int position, float realMultiplier) {
    realArray[position] *= realMultiplier;
    imaginaryArray[position] *= realMultiplier;
  }

  //全部乘法
  public void multiplyAll(float realMultiplier) {
    for (int i = 0; i < size; i++) {multiply(i, realMultiplier);}
  }

  //除法
  public void divide(int position, ComplexNumber divisor) {
    if (divisor != null) {
      float sumBase = (float) (Math.pow(divisor.getRealPart(), 2) +
          Math.pow(divisor.getImaginaryPart(), 2));
      float newRealPart = (realArray[position] * divisor.getRealPart() +
          imaginaryArray[position] * divisor.getImaginaryPart()) / sumBase;
      float newImaginaryPart = (imaginaryArray[position] * divisor.getRealPart() -
          realArray[position] * divisor.getImaginaryPart()) / sumBase;
      realArray[position] = newRealPart;
      imaginaryArray[position] = newImaginaryPart;
    }
  }

  //全部除法
  public void divideAll(ComplexNumber divisor) {
    if (divisor != null) {
      for (int i = 0; i < size; i++) {divide(i, divisor);}
    }
  }

  //共轭
  public void conjugate(int position) {
    imaginaryArray[position] = -imaginaryArray[position];
  }

  //全部共轭
  public void conjugateAll() {
    for (int i = 0; i < size; i++) {conjugate(i);}
  }

  public String toString(int position) {
    if (imaginaryArray[position] < 0) {
      return realArray[position] + "" + imaginaryArray[position] + "i";
    }
    return realArray[position] + "+" + imaginaryArray[position] + "i";
  }

}