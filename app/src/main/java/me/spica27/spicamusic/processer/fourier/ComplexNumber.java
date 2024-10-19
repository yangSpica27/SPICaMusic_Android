package me.spica27.spicamusic.processer.fourier;

public class ComplexNumber {
  private float realPart;
  private float imaginaryPart;

  public ComplexNumber(float realPart, float imaginaryPart) {
    this.realPart = realPart;
    this.imaginaryPart = imaginaryPart;
  }

  //实部
  public float getRealPart() {
    return realPart;
  }

  public void setRealPart(float realPart) {
    this.realPart = realPart;
  }

  //虚部
  public float getImaginaryPart() {
    return imaginaryPart;
  }

  public void setImaginaryPart(float imaginaryPart) {
    this.imaginaryPart = imaginaryPart;
  }

  //设置
  public void setData(float realPart, float imaginaryPart) {
    this.realPart = realPart;
    this.imaginaryPart = imaginaryPart;
  }

  //清除
  public void clear() {
    this.realPart = 0;
    this.imaginaryPart = 0;
  }

  //幅度
  public float getAmplitude() {
    return (float) Math.sqrt(Math.pow(realPart, 2) + Math.pow(imaginaryPart, 2));
  }

  //相位
  public float getPhase() {
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

  //加法
  public ComplexNumber addNew(ComplexNumber addend) {
    if (addend == null) {return new ComplexNumber(this.realPart, this.imaginaryPart);}
    return new ComplexNumber(this.realPart + addend.realPart,
        this.imaginaryPart + addend.imaginaryPart);
  }

  //减法
  public ComplexNumber subtractNew(ComplexNumber subtrahend) {
    if (subtrahend == null) {return new ComplexNumber(this.realPart, this.imaginaryPart);}
    return new ComplexNumber(this.realPart - subtrahend.realPart,
        this.imaginaryPart - subtrahend.imaginaryPart);
  }

  //乘法
  public ComplexNumber multiplyNew(ComplexNumber multiplier) {
    if (multiplier == null) {return new ComplexNumber(this.realPart, this.imaginaryPart);}
    float newRealPart = this.realPart * multiplier.realPart -
        this.imaginaryPart * multiplier.imaginaryPart;
    float newImaginaryPart = this.realPart * multiplier.imaginaryPart +
        this.imaginaryPart * multiplier.realPart;
    return new ComplexNumber(newRealPart, newImaginaryPart);
  }

  public ComplexNumber multiplyNew(float multiplier) {
    return new ComplexNumber(this.realPart * multiplier, this.imaginaryPart * multiplier);
  }

  //除法
  public ComplexNumber divideNew(ComplexNumber divisor) {
    if (divisor == null) {return new ComplexNumber(this.realPart, this.imaginaryPart);}
    float sumBase = (float) (Math.pow(divisor.realPart, 2) + Math.pow(divisor.imaginaryPart, 2));
    float newRealPart =
        (this.realPart * divisor.realPart + this.imaginaryPart * divisor.imaginaryPart) / sumBase;
    float newImaginaryPart =
        (this.realPart * divisor.imaginaryPart - this.imaginaryPart * divisor.realPart) / sumBase;
    return new ComplexNumber(newRealPart, newImaginaryPart);
  }

  //共轭
  public ComplexNumber conjugateNew() {
    return new ComplexNumber(this.realPart, -this.imaginaryPart);
  }

  //加法
  public ComplexNumber add(ComplexNumber addend) {
    if (addend == null) {return this;}
    this.realPart += addend.realPart;
    this.imaginaryPart += addend.imaginaryPart;
    return this;
  }

  //减法
  public ComplexNumber subtract(ComplexNumber subtrahend) {
    if (subtrahend == null) {return this;}
    this.realPart -= subtrahend.realPart;
    this.imaginaryPart -= subtrahend.imaginaryPart;
    return this;
  }

  //乘法
  public ComplexNumber multiply(ComplexNumber multiplier) {
    if (multiplier == null) {return this;}
    float newRealPart = this.realPart * multiplier.realPart -
        this.imaginaryPart * multiplier.imaginaryPart;
    float newImaginaryPart = this.realPart * multiplier.imaginaryPart +
        this.imaginaryPart * multiplier.realPart;
    this.realPart = newRealPart;
    this.imaginaryPart = newImaginaryPart;
    return this;
  }

  public ComplexNumber multiply(float multiplier) {
    this.realPart *= multiplier;
    this.imaginaryPart *= multiplier;
    return this;
  }

  //除法
  public ComplexNumber divide(ComplexNumber divisor) {
    if (divisor == null) {return this;}
    float sumBase = (float) (Math.pow(divisor.realPart, 2) + Math.pow(divisor.imaginaryPart, 2));
    float newRealPart =
        (this.realPart * divisor.realPart + this.imaginaryPart * divisor.imaginaryPart) / sumBase;
    float newImaginaryPart =
        (this.imaginaryPart * divisor.realPart - this.realPart * divisor.imaginaryPart) / sumBase;
    this.realPart = newRealPart;
    this.imaginaryPart = newImaginaryPart;
    return this;
  }

  //共轭
  public ComplexNumber conjugate() {
    this.imaginaryPart = -this.imaginaryPart;
    return this;
  }

  public String toString() {
    if (imaginaryPart < 0) {
      return realPart + "" + imaginaryPart + "i";
    }
    return realPart + "+" + imaginaryPart + "i";
  }
}