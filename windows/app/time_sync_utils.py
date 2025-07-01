"""
Utility module for timestamp synchronization across devices.
This ensures that timestamps from different devices can be aligned
to a common timeline.
"""

def calculate_offset(local_timestamp, reference_timestamp):
    """
    Calculate the offset between a local timestamp and a reference timestamp.
    This offset can be used to convert local timestamps to the reference timeline.
    
    Args:
        local_timestamp: The local device timestamp
        reference_timestamp: The reference timestamp (e.g., from the PC)
    
    Returns:
        The offset to add to local timestamps to get reference timestamps
    """
    return reference_timestamp - local_timestamp

def convert_to_reference_time(local_timestamp, offset):
    """
    Convert a local timestamp to a reference timestamp using an offset.
    
    Args:
        local_timestamp: The local device timestamp
        offset: The offset calculated using calculate_offset()
    
    Returns:
        The timestamp in the reference timeline
    """
    return local_timestamp + offset

def calculate_round_trip_time(ping_timestamp, pong_timestamp):
    """
    Calculate the round-trip time for a ping-pong synchronization.
    This can be used to estimate the network latency.
    
    Args:
        ping_timestamp: The timestamp when the ping was sent
        pong_timestamp: The timestamp when the pong was received
    
    Returns:
        The round-trip time in milliseconds
    """
    return pong_timestamp - ping_timestamp

def estimate_one_way_latency(round_trip_time):
    """
    Estimate the one-way network latency based on the round-trip time.
    This assumes symmetric network conditions.
    
    Args:
        round_trip_time: The round-trip time calculated using calculate_round_trip_time()
    
    Returns:
        The estimated one-way latency in milliseconds
    """
    return round_trip_time // 2  # Integer division for consistency with Java

def calculate_adjusted_offset(local_timestamp, reference_timestamp, one_way_latency):
    """
    Calculate a more accurate offset by accounting for network latency.
    
    Args:
        local_timestamp: The local device timestamp
        reference_timestamp: The reference timestamp (e.g., from the PC)
        one_way_latency: The estimated one-way latency
    
    Returns:
        The offset adjusted for network latency
    """
    return reference_timestamp - local_timestamp - one_way_latency

def perform_ntp_like_sync(ping_func, pong_handler, num_samples=5):
    """
    Perform an NTP-like synchronization with multiple samples to improve accuracy.
    
    Args:
        ping_func: A function that sends a ping and returns the local send timestamp
        pong_handler: A function that processes the pong response and returns (local_receive_time, remote_time)
        num_samples: Number of ping-pong exchanges to perform
    
    Returns:
        The calculated offset and estimated error margin
    """
    offsets = []
    round_trip_times = []
    
    for _ in range(num_samples):
        # Send ping and get local send time
        local_send_time = ping_func()
        
        # Wait for pong and get local receive time and remote timestamp
        local_receive_time, remote_time = pong_handler()
        
        # Calculate round trip time
        rtt = calculate_round_trip_time(local_send_time, local_receive_time)
        round_trip_times.append(rtt)
        
        # Calculate offset adjusted for estimated one-way latency
        one_way = estimate_one_way_latency(rtt)
        offset = calculate_adjusted_offset(local_send_time + one_way, remote_time, 0)
        offsets.append(offset)
    
    # Filter out samples with high RTT (optional)
    valid_samples = [(offsets[i], round_trip_times[i]) for i in range(len(offsets))]
    valid_samples.sort(key=lambda x: x[1])  # Sort by RTT
    
    # Use the median or average of the best samples
    if valid_samples:
        best_samples = valid_samples[:max(1, len(valid_samples) // 2)]  # Use best half
        final_offset = sum(sample[0] for sample in best_samples) / len(best_samples)
        error_margin = min(sample[1] for sample in best_samples) / 2
        return final_offset, error_margin
    
    return 0, float('inf')  # Fallback if no valid samples