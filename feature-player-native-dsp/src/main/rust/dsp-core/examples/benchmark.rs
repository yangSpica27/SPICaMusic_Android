use spica_dsp_core::{pcm::Encoding, Controls, Engine, Parameters};
use std::sync::Arc;
use std::time::Instant;

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let rate: u32 = args.get(1).map(|v| v.parse().unwrap()).unwrap_or(48000);
    let channels: usize = args.get(2).map(|v| v.parse().unwrap()).unwrap_or(2);
    let mode = args.get(3).map(String::as_str).unwrap_or("all");
    let controls = Arc::new(Controls::default());
    controls.publish(Parameters {
        eq: mode == "eq" || mode == "all",
        loudness: mode == "loudness" || mode == "all",
        target_lufs: -14.0,
        gains: [3.0; 10],
    });
    controls.analysis.set_enabled(mode == "all");
    controls.analysis.set_playing(true);
    let mut engine = Engine::new(controls, rate, channels, Encoding::F32, 32768).unwrap();
    let input: Vec<u8> = (0..1024)
        .flat_map(|i| {
            let value =
                (0.1 * (2.0 * std::f64::consts::PI * 997.0 * i as f64 / rate as f64).sin()) as f32;
            (0..channels).flat_map(move |_| value.to_le_bytes())
        })
        .collect();
    let mut output = vec![0; input.len()];
    for _ in 0..64 {
        engine.process(&input, &mut output).unwrap();
    }
    let mut times = Vec::with_capacity(2000);
    for _ in 0..2000 {
        let start = Instant::now();
        std::hint::black_box(
            engine
                .process(std::hint::black_box(&input), &mut output)
                .unwrap(),
        );
        times.push(start.elapsed().as_nanos() as f64 / 1000.0);
    }
    times.sort_by(f64::total_cmp);
    println!("{{\"engine\":\"rust\",\"rate\":{rate},\"channels\":{channels},\"mode\":\"{mode}\",\"p50_us\":{},\"p99_us\":{}}}",times[1000],times[1980]);
}
