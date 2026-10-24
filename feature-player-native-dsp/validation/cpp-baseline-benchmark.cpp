#include "spica/dsp/dsp_engine.h"
#include <chrono>
#include <iostream>
#include <vector>
#include <algorithm>
#include <cmath>
#include <string>
int main(int argc,char** argv) {
    const int rate=argc>1?std::stoi(argv[1]):48000;
    const int channels=argc>2?std::stoi(argv[2]):2;
    const std::string mode=argc>3?argv[3]:"all";
    spica::dsp::DspEngine engine;
    if(!engine.configure(rate,channels,spica::dsp::PcmEncoding::PcmFloat,32768))return 1;
    spica::dsp::DspParameters parameters;
    parameters.eqEnabled=mode=="eq"||mode=="all";
    parameters.loudnessEnabled=mode=="loudness"||mode=="all";
    parameters.fftEnabled=mode=="all";
    parameters.eqGainsDb.fill(3.0f);
    engine.setParameters(parameters);
    engine.setPlaybackActive(true);
    std::vector<float> input(1024*channels),output(input.size());
    for(int i=0;i<1024;++i)for(int ch=0;ch<channels;++ch)input[i*channels+ch]=0.1*std::sin(2*3.14159265358979323846*997*i/rate);
    const auto run=[&](){return engine.process(reinterpret_cast<const std::uint8_t*>(input.data()),input.size()*4,reinterpret_cast<std::uint8_t*>(output.data()),output.size()*4);};
    for(int i=0;i<64;++i)run();
    std::vector<double> times;
    times.reserve(2000);
    for(int i=0;i<2000;++i){
        auto start=std::chrono::steady_clock::now();
        if(run()!=static_cast<int>(input.size()*4))return 2;
        times.push_back(std::chrono::duration<double,std::micro>(std::chrono::steady_clock::now()-start).count());
    }
    std::sort(times.begin(),times.end());
    std::cout<<"{\"engine\":\"cpp\",\"rate\":"<<rate<<",\"channels\":"<<channels<<",\"mode\":\""<<mode<<"\",\"p50_us\":"<<times[1000]<<",\"p99_us\":"<<times[1980]<<"}\n";
}
