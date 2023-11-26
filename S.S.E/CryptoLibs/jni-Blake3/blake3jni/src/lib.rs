use jni::JNIEnv;
use jni::objects::{JClass, JByteBuffer};
use jni::sys::{jlong, jint, jbyteArray};
use std::convert::TryInto;
use blake3::Hasher;

#[no_mangle]
pub extern fn Java_io_lktk_JNIRust_create_1hasher(_env: JNIEnv, _class: JClass) -> jlong
{    
    let hasher = Hasher::new();
    let boxed_hasher: Box<Hasher> = Box::new(hasher);
    let ptr = Box::into_raw(boxed_hasher);

    return ptr as i64;
}

#[no_mangle]
pub extern fn Java_io_lktk_JNIRust_create_1hasher_1keyed(env: JNIEnv, _class: JClass, key: jbyteArray) -> jlong
{    
    let byte_slice : &[u8] = &JNIEnv::convert_byte_array(&env, key).unwrap();
    let key_converted : &[u8; 32] = byte_slice.try_into().unwrap();
    
    let hasher = Hasher::new_keyed(key_converted);
    let boxed_hasher: Box<Hasher> = Box::new(hasher);
    let ptr = Box::into_raw(boxed_hasher);

    return ptr as i64;
}

#[no_mangle]
pub extern fn Java_io_lktk_JNIRust_destroy_1hasher(_env: JNIEnv, _class: JClass, hp: jlong)
{
    unsafe { Box::from_raw(hp as *mut Hasher) };
}
 
#[no_mangle]
pub extern fn Java_io_lktk_JNIRust_blake3_1hasher_1update(env: JNIEnv, _class: JClass, hp: jlong, data: jbyteArray, data_length: jlong)
{    
    let data_ptr = JNIEnv::get_primitive_array_critical(&env, data, jni::objects::ReleaseMode::NoCopyBack).unwrap().as_ptr();
    update_internal(hp, data_ptr as *const u8, data_length as usize);
}

#[no_mangle]
pub extern fn Java_io_lktk_JNIRust_blake3_1hasher_1updatefb(env: JNIEnv, _class: JClass, hp: jlong, data: JByteBuffer, data_length: jlong)
{    
    let data_ptr = JNIEnv::get_direct_buffer_address(&env, data).unwrap().as_ptr();
    update_internal(hp, data_ptr as *const u8, data_length as usize);
}

fn update_internal(hp: jlong, data_ptr: *const u8, data_length: usize)
{
    let byte_slice = unsafe { std::slice::from_raw_parts(data_ptr, data_length) };
    let mut hasher = unsafe { Box::from_raw(hp as *mut Hasher) };
    if data_length > 131072 {
        hasher.update_rayon(&byte_slice);
    }
    else {
        hasher.update(&byte_slice);
    }
    Box::into_raw(Box::new(hasher));
}

#[no_mangle]
pub extern fn Java_io_lktk_JNIRust_blake3_1hasher_1finalize(env: JNIEnv, _class: JClass, hp: jlong, output_length: jint) -> jbyteArray
{    
    let hasher = unsafe { Box::from_raw(hp as *mut Hasher) };
    let mut output = vec![0; output_length as usize];
    let mut output_reader = hasher.finalize_xof();
    output_reader.fill(&mut output);

    return JNIEnv::byte_array_from_slice(&env, &output).unwrap();
}

