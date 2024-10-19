package me.spica27.spicamusic.processer.fourier;

public abstract class Fourier {
  private int size;
  private ComplexNumberArray complexNumberArray;

  ;
  private int flyTime;
  private int[] index;
  private UnitRoot root;
  private float[] sortedFFTData;
  private float[] sortedIFFTData;
  public Fourier(int size) {
    this.size = size;
    complexNumberArray = new ComplexNumberArray(size);
    flyTime = getBinaryOne(size - 1);
    index = createIndex(size);
    root = new UnitRoot(size);
    sortedFFTData = new float[size];
    sortedIFFTData = new float[size];
  }

  private int sampleFrequency() {
    return getSampleFrequency();
  }

  //创建奇偶分类后的索引
  private int[] createIndex(int n) {
    int[] index = new int[n];
    int left = 0;
    int middle = n / 2;
    int right = n;
    index[left] = 0;
    index[middle] = 1;
    createIndexMerge(index, left, middle, 1);
    createIndexMerge(index, middle, right, 1);
    return index;
  }

  //递归调用
  private void createIndexMerge(int[] index, int left, int right, int multiple) {
    if (right - left <= 1) return;
    int value = (int) Math.pow(2, multiple);
    int middle = (right + left) / 2;
    index[middle] = (index[left] + value);
    createIndexMerge(index, left, middle, multiple + 1);
    createIndexMerge(index, middle, right, multiple + 1);

  }

  //获取一个数对应二进制形式中
  private int getBinaryOne(int num) {
    int count = 0;
    while (num != 0) {
      count++;
      num = num & (num - 1);
    }
    return count;
  }

  //根据索引内容将数据进行重排序
  //用于FFT
  private float[] sortDataByIndexFFT(float[] data) {
    clearArray(sortedFFTData);
    for (int i = 0; i < index.length; i++) {
      int p = index[i];
      sortedFFTData[i] = data[p];
    }
    return sortedFFTData;
  }

  //用于IFFT
  private float[] sortDataByIndexIFFT(float[] data) {
    clearArray(sortedIFFTData);
    for (int i = 0; i < index.length; i++) {
      int p = index[i];
      sortedIFFTData[p] = data[i];
    }
    return sortedIFFTData;
  }

  //清空数组数据
  private void clearArray(float[] array) {
    int len = array.length;
    for (int i = 0; i < len; i++) {array[i] = 0;}
  }

  //将实数形式的数据转化为复数形式
  private ComplexNumberArray createComplexNumberArray(float[] data) {
    complexNumberArray.clearAll();
    complexNumberArray.setAllComplexNumber(data, null);
    return complexNumberArray;
  }

  public ComplexNumberArray fft(float[] data) {
    return fft(data, 2);
  }

  //快速傅里叶变换
  public ComplexNumberArray fft(float[] data, int value) {
    sortedFFTData = sortDataByIndexFFT(data);
    ComplexNumberArray complexNumberArray = createComplexNumberArray(sortedFFTData);

    for (int i = 1; i <= flyTime; i++) {
      //每次蝶形运算中数据的数量
      int teamSize = (int) (Math.pow(value, i));
      //大蝶形运算中需要计算的组数
      int teams = size / teamSize;
      for (int j = 0; j < teams; j++) {
        int start1 = j * teamSize;
        int start2 = start1 + teamSize / value;
        for (int k = 0; k < teamSize / value; k++) {
          complexNumberArray.multiply(start2, root.getUnitRoot(teamSize, k));
          ComplexNumber cNum = complexNumberArray.getComplexNumber(start2);
          complexNumberArray.setComplexNumber(start2, complexNumberArray.getRealPart(start1),
              complexNumberArray.getImaginaryPart(start1));
          complexNumberArray.add(start1, cNum);
          complexNumberArray.subtract(start2, cNum);

          start1++;
          start2++;
        }
      }
    }
    return complexNumberArray;
  }

  //快速傅里叶逆变换
  public float[] ifft(ComplexNumberArray complexNumberArray) {
    for (int i = flyTime; i >= 0; i--) {
      //每次蝶形运算中数据的数量
      int teamSize = (int) (Math.pow(2, i));
      //大蝶形运算中需要计算的组数
      int teams = size / teamSize;
      for (int j = 0; j < teams; j++) {
        int start1 = j * teamSize;
        int start2 = start1 + teamSize / 2;
        for (int k = 0; k < teamSize / 2; k++) {
          ComplexNumber cNum = complexNumberArray.getComplexNumber(start2);
          complexNumberArray.setComplexNumber(start2, complexNumberArray.getRealPart(start1),
              complexNumberArray.getImaginaryPart(start1));
          complexNumberArray.add(start1, cNum);
          complexNumberArray.subtract(start2, cNum);
          complexNumberArray.multiply(start1, 0.5f);
          complexNumberArray.multiply(start2,
              root.getUnitRoot(teamSize, k).multiplyNew(0.5f).conjugate());

          start1++;
          start2++;
        }
      }
    }
    return sortDataByIndexIFFT(complexNumberArray.getAllRealPart());
  }

  protected abstract int getSampleFrequency();

  public static class Analyzer {
    private Fourier fourier;

    public Analyzer(Fourier fourier) {
      this.fourier = fourier;
    }

    //获取最大幅值
    public float getMaxAmplitude(ComplexNumberArray complexNumberArray) {
      float amplitude = 0;
      if (complexNumberArray != null) {
        float[] amplitudes = complexNumberArray.getAllAmplitude();
        amplitude = max(amplitudes, 0);
      }
      return amplitude;
    }

    //获取除了直流外的最大幅度
    public float getMaxAmplitudeExceptDirectComponent(ComplexNumberArray complexNumberArray) {
      float amplitude = 0;
      if (complexNumberArray != null) {
        float[] amplitudes = complexNumberArray.getAllAmplitude();
        amplitude = max(amplitudes, 1);
      }
      return amplitude;
    }

    //获取最大幅值处的频率
    public float getFrequencyAtMaxAmplitude(ComplexNumberArray complexNumberArray) {
      float frequency = 0;
      if (complexNumberArray != null) {
        float[] amplitudes = complexNumberArray.getAllAmplitude();
        int index = getIndexAtMaxValue(amplitudes, 0);
        frequency = index * getFrequencyResolution();
      }
      return frequency;
    }

    //获取除了直流外最大幅度处的频率
    public float getFrequencyAtMaxAmplitudeExceptDirectComponent(ComplexNumberArray complexNumberArray) {
      float frequency = 0;
      if (complexNumberArray != null) {
        float[] amplitudes = complexNumberArray.getAllAmplitude();
        int index = getIndexAtMaxValue(amplitudes, 1);
        frequency = index * getFrequencyResolution();
      }
      return frequency;
    }

    //获取最大相位
    public float getMaxPhase(ComplexNumberArray complexNumberArray) {
      float phase = 0;
      if (complexNumberArray != null) {
        float[] phases = complexNumberArray.getAllPhase();
        phase = max(phases, 0);
      }
      return phase;
    }

    //获取最小相位
    public float getMinPhase(ComplexNumberArray complexNumberArray) {
      float phase = 0;
      if (complexNumberArray != null) {
        float[] phases = complexNumberArray.getAllPhase();
        phase = min(phases, 0);
      }
      return phase;
    }

    //获取最大相位处的频率
    public float getFrequencyAtMaxPhase(ComplexNumberArray complexNumberArray) {
      float frequency = 0;
      if (complexNumberArray != null) {
        float[] phases = complexNumberArray.getAllPhase();
        int index = getIndexAtMaxValue(phases, 0);
        frequency = index * getFrequencyResolution();
      }
      return frequency;
    }

    //获取最小相位处的频率
    public float getFrequencyAtMinPhase(ComplexNumberArray complexNumberArray) {
      float frequency = 0;
      if (complexNumberArray != null) {
        float[] phases = complexNumberArray.getAllPhase();
        int index = getIndexAtMinValue(phases, 0);
        frequency = index * getFrequencyResolution();
      }
      return frequency;
    }

    //获取频率分辨率
    public float getFrequencyResolution() {
      return fourier.sampleFrequency() / fourier.size;
    }

    //获取归一化频率
    public float getNormalizeFrequency(float frequency) {
      return frequency / fourier.sampleFrequency();
    }

    //获取圆频率
    public float getCircularFrequency(float frequency) {
      return (float) (getNormalizeFrequency(frequency) * Math.PI * 2);
    }

    //获取角频率
    public float getAngularFrequency(float frequency) {
      return (float) (frequency * Math.PI * 2);
    }

    //从startIndex开始，求一个数组的最大值
    private float max(float[] nums, int startIndex) {
      float maxNum = nums[startIndex];
      int len = nums.length;
      for (int i = startIndex + 1; i < len; i++) {
        if (nums[i] > maxNum) {
          maxNum = nums[i];
        }
      }
      return maxNum;
    }

    //从startIndex开始，求一个数组的最小值
    private float min(float[] nums, int startIndex) {
      float minNum = nums[startIndex];
      int len = nums.length;
      for (int i = startIndex + 1; i < len; i++) {
        if (nums[i] < minNum) {
          minNum = nums[i];
        }
      }
      return minNum;
    }

    //从startIndex开始，求一个数组最大值对应的索引
    private int getIndexAtMaxValue(float[] nums, int startIndex) {
      float maxNum = nums[startIndex];
      int index = startIndex;
      for (int i = startIndex + 1; i < nums.length; i++) {
        if (nums[i] > maxNum) {
          maxNum = nums[i];
          index = i;
        }
      }
      return index;
    }

    //从startIndex开始，求一个数组最小值对应的索引
    private int getIndexAtMinValue(float[] nums, int startIndex) {
      float minNum = nums[startIndex];
      int index = startIndex;
      for (int i = startIndex + 1; i < nums.length; i++) {
        if (nums[i] < minNum) {
          minNum = nums[i];
          index = i;
        }
      }
      return index;
    }
  }
}