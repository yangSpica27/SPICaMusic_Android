package me.spica27.spicamusic.processer.fourier;

import java.util.ArrayList;
import java.util.List;

public class UnitRoot {
  private int n;
  private List<ComplexNumber> rootList;

  public UnitRoot(int n) {
    this.n = n;
    rootList = new ArrayList<>();
    calculate();
  }

  private void calculate() {
    for (int k = 0; k <= n; k++) {
      float unit = (float) (2 * Math.PI * k / n);
      rootList.add(new ComplexNumber((float) Math.cos(unit), -(float) Math.sin(unit)));
    }
  }

  public List<ComplexNumber> getAllUnitRoots() {
    return rootList;
  }

  public ComplexNumber getUnitRoot(int n, int k) {
    if (this.n == n) {
      return rootList.get(k);
    } else if (this.n > n) {
      int multiple = this.n / n;
      return rootList.get(k * multiple);
    } else if (this.n < n) {
      int multiple = n / this.n;
      return rootList.get(k / multiple);
    }
    return null;
  }

}