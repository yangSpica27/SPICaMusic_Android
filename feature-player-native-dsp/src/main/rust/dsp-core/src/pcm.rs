#[derive(Clone, Copy, Debug, PartialEq)]
pub enum Encoding {
    U8,
    I16,
    I16Be,
    I24,
    I24Be,
    I32,
    I32Be,
    F32,
}
impl Encoding {
    pub fn from_media3(value: i32) -> Option<Self> {
        Some(match value {
            3 => Self::U8,
            2 => Self::I16,
            268435456 => Self::I16Be,
            21 => Self::I24,
            1342177280 => Self::I24Be,
            22 => Self::I32,
            1610612736 => Self::I32Be,
            4 => Self::F32,
            _ => return None,
        })
    }
    pub fn bytes(self) -> usize {
        match self {
            Self::U8 => 1,
            Self::I16 | Self::I16Be => 2,
            Self::I24 | Self::I24Be => 3,
            _ => 4,
        }
    }
    pub fn decode(self, data: &[u8]) -> f64 {
        let value = match self {
            Self::U8 => (data[0] as f64 - 128.0) / 128.0,
            Self::I16 => i16::from_le_bytes([data[0], data[1]]) as f64 / 32768.0,
            Self::I16Be => i16::from_be_bytes([data[0], data[1]]) as f64 / 32768.0,
            Self::I24 | Self::I24Be => {
                let raw = if self == Self::I24 {
                    (data[0] as i32) | ((data[1] as i32) << 8) | ((data[2] as i32) << 16)
                } else {
                    (data[2] as i32) | ((data[1] as i32) << 8) | ((data[0] as i32) << 16)
                };
                ((raw << 8) >> 8) as f64 / 8388608.0
            }
            Self::I32 => i32::from_le_bytes(data[..4].try_into().unwrap()) as f64 / 2147483648.0,
            Self::I32Be => i32::from_be_bytes(data[..4].try_into().unwrap()) as f64 / 2147483648.0,
            Self::F32 => f32::from_le_bytes(data[..4].try_into().unwrap()) as f64,
        };
        if value.is_finite() {
            value
        } else {
            0.0
        }
    }
    pub fn encode(self, value: f64, out: &mut [u8]) {
        let value = if value.is_finite() {
            value.clamp(-1.0, 1.0)
        } else {
            0.0
        };
        match self {
            Self::U8 => out[0] = (value * 128.0 + 128.0).round().clamp(0.0, 255.0) as u8,
            Self::I16 | Self::I16Be => {
                let value = (value * 32768.0).round().clamp(-32768.0, 32767.0) as i16;
                out[..2].copy_from_slice(&if self == Self::I16 {
                    value.to_le_bytes()
                } else {
                    value.to_be_bytes()
                });
            }
            Self::I24 | Self::I24Be => {
                let value = (value * 8388608.0).round().clamp(-8388608.0, 8388607.0) as i32;
                if self == Self::I24 {
                    out[..3].copy_from_slice(&value.to_le_bytes()[..3]);
                } else {
                    out[..3].copy_from_slice(&value.to_be_bytes()[1..]);
                }
            }
            Self::I32 | Self::I32Be => {
                let value = (value * 2147483648.0)
                    .round()
                    .clamp(-2147483648.0, 2147483647.0) as i32;
                out[..4].copy_from_slice(&if self == Self::I32 {
                    value.to_le_bytes()
                } else {
                    value.to_be_bytes()
                });
            }
            Self::F32 => out[..4].copy_from_slice(&(value as f32).to_le_bytes()),
        }
    }
}
