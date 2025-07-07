#!/usr/bin/env python3
"""
Advanced configuration management system for the high-precision C++ hardware backend.

This module provides comprehensive configuration management with environment-specific
settings, runtime parameter adjustment, and validation.
"""

import sys
import os
import json
import configparser
from typing import Dict, Any, Optional, List
from dataclasses import dataclass, asdict
from pathlib import Path
import logging

@dataclass
class HardwareConfig:
    """Configuration for hardware devices."""
    shimmer_com_port: str = "COM3"
    shimmer_baud_rate: int = 115200
    shimmer_timeout_ms: int = 1000
    shimmer_buffer_size: int = 1024
    
    webcam_index: int = 0
    webcam_width: int = 3840
    webcam_height: int = 2160
    webcam_fps: int = 30
    webcam_buffer_size: int = 1

@dataclass
class PerformanceConfig:
    """Configuration for performance monitoring and thresholds."""
    max_latency_ms: float = 1.0
    max_jitter_ms: float = 0.5
    min_data_rate_hz: int = 100
    min_frame_rate_fps: int = 25
    
    monitoring_interval_ms: int = 10
    display_update_interval_s: float = 1.0
    max_samples: int = 1000
    alert_history_size: int = 100

@dataclass
class SystemConfig:
    """Configuration for system-level settings."""
    log_level: str = "INFO"
    log_file: Optional[str] = None
    data_output_dir: str = "./data"
    backup_enabled: bool = True
    backup_interval_s: int = 300
    
    cpu_affinity: Optional[List[int]] = None
    thread_priority: str = "normal"  # normal, high, realtime
    memory_limit_mb: Optional[int] = None

@dataclass
class ApplicationConfig:
    """Complete application configuration."""
    hardware: HardwareConfig
    performance: PerformanceConfig
    system: SystemConfig
    
    def __post_init__(self):
        """Validate configuration after initialization."""
        self._validate()
    
    def _validate(self):
        """Validate configuration values."""
        # Hardware validation
        if self.hardware.shimmer_baud_rate not in [9600, 19200, 38400, 57600, 115200]:
            raise ValueError(f"Invalid baud rate: {self.hardware.shimmer_baud_rate}")
        
        if self.hardware.webcam_width <= 0 or self.hardware.webcam_height <= 0:
            raise ValueError("Invalid webcam resolution")
        
        # Performance validation
        if self.performance.max_latency_ms <= 0:
            raise ValueError("Max latency must be positive")
        
        if self.performance.max_jitter_ms <= 0:
            raise ValueError("Max jitter must be positive")
        
        # System validation
        if self.system.log_level not in ["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"]:
            raise ValueError(f"Invalid log level: {self.system.log_level}")
        
        if self.system.thread_priority not in ["normal", "high", "realtime"]:
            raise ValueError(f"Invalid thread priority: {self.system.thread_priority}")

class ConfigManager:
    """
    Advanced configuration management system.
    
    Supports multiple configuration sources, environment-specific settings,
    runtime updates, and validation.
    """
    
    def __init__(self, config_dir: str = "./config"):
        self.config_dir = Path(config_dir)
        self.config_dir.mkdir(exist_ok=True)
        
        self.config: Optional[ApplicationConfig] = None
        self.config_file: Optional[Path] = None
        self.environment: str = os.getenv("HARDWARE_ENV", "development")
        
        # Configuration file paths
        self.default_config_file = self.config_dir / "default.json"
        self.env_config_file = self.config_dir / f"{self.environment}.json"
        self.user_config_file = self.config_dir / "user.json"
        self.runtime_config_file = self.config_dir / "runtime.json"
        
        self._setup_logging()
        
    def _setup_logging(self):
        """Setup logging for configuration management."""
        self.logger = logging.getLogger(__name__)
        
    def load_config(self) -> ApplicationConfig:
        """
        Load configuration from multiple sources with precedence:
        1. Runtime configuration (highest priority)
        2. User configuration
        3. Environment-specific configuration
        4. Default configuration (lowest priority)
        """
        self.logger.info(f"Loading configuration for environment: {self.environment}")
        
        # Start with default configuration
        config_dict = self._load_default_config()
        
        # Override with environment-specific config
        env_config = self._load_env_config()
        if env_config:
            config_dict = self._merge_configs(config_dict, env_config)
            
        # Override with user config
        user_config = self._load_user_config()
        if user_config:
            config_dict = self._merge_configs(config_dict, user_config)
            
        # Override with runtime config
        runtime_config = self._load_runtime_config()
        if runtime_config:
            config_dict = self._merge_configs(config_dict, runtime_config)
        
        # Create configuration object
        self.config = ApplicationConfig(
            hardware=HardwareConfig(**config_dict.get("hardware", {})),
            performance=PerformanceConfig(**config_dict.get("performance", {})),
            system=SystemConfig(**config_dict.get("system", {}))
        )
        
        self.logger.info("Configuration loaded successfully")
        return self.config
    
    def _load_default_config(self) -> Dict[str, Any]:
        """Load default configuration."""
        default_config = {
            "hardware": asdict(HardwareConfig()),
            "performance": asdict(PerformanceConfig()),
            "system": asdict(SystemConfig())
        }
        
        # Save default config if it doesn't exist
        if not self.default_config_file.exists():
            self._save_config_file(self.default_config_file, default_config)
            
        return self._load_config_file(self.default_config_file) or default_config
    
    def _load_env_config(self) -> Optional[Dict[str, Any]]:
        """Load environment-specific configuration."""
        return self._load_config_file(self.env_config_file)
    
    def _load_user_config(self) -> Optional[Dict[str, Any]]:
        """Load user-specific configuration."""
        return self._load_config_file(self.user_config_file)
    
    def _load_runtime_config(self) -> Optional[Dict[str, Any]]:
        """Load runtime configuration."""
        return self._load_config_file(self.runtime_config_file)
    
    def _load_config_file(self, file_path: Path) -> Optional[Dict[str, Any]]:
        """Load configuration from a JSON file."""
        if not file_path.exists():
            return None
            
        try:
            with open(file_path, 'r') as f:
                config = json.load(f)
            self.logger.debug(f"Loaded config from {file_path}")
            return config
        except Exception as e:
            self.logger.error(f"Error loading config from {file_path}: {e}")
            return None
    
    def _save_config_file(self, file_path: Path, config: Dict[str, Any]):
        """Save configuration to a JSON file."""
        try:
            with open(file_path, 'w') as f:
                json.dump(config, f, indent=2)
            self.logger.debug(f"Saved config to {file_path}")
        except Exception as e:
            self.logger.error(f"Error saving config to {file_path}: {e}")
    
    def _merge_configs(self, base: Dict[str, Any], override: Dict[str, Any]) -> Dict[str, Any]:
        """Merge two configuration dictionaries."""
        result = base.copy()
        
        for key, value in override.items():
            if key in result and isinstance(result[key], dict) and isinstance(value, dict):
                result[key] = self._merge_configs(result[key], value)
            else:
                result[key] = value
                
        return result
    
    def save_user_config(self, config: ApplicationConfig):
        """Save configuration as user configuration."""
        config_dict = {
            "hardware": asdict(config.hardware),
            "performance": asdict(config.performance),
            "system": asdict(config.system)
        }
        self._save_config_file(self.user_config_file, config_dict)
        self.logger.info("User configuration saved")
    
    def update_runtime_config(self, updates: Dict[str, Any]):
        """Update runtime configuration with partial updates."""
        runtime_config = self._load_runtime_config() or {}
        runtime_config = self._merge_configs(runtime_config, updates)
        
        self._save_config_file(self.runtime_config_file, runtime_config)
        self.logger.info("Runtime configuration updated")
        
        # Reload configuration
        self.load_config()
    
    def get_config(self) -> ApplicationConfig:
        """Get current configuration, loading if necessary."""
        if self.config is None:
            self.load_config()
        return self.config
    
    def create_environment_config(self, environment: str, config_overrides: Dict[str, Any]):
        """Create configuration for a specific environment."""
        env_file = self.config_dir / f"{environment}.json"
        self._save_config_file(env_file, config_overrides)
        self.logger.info(f"Created configuration for environment: {environment}")
    
    def list_environments(self) -> List[str]:
        """List available environment configurations."""
        environments = []
        for file_path in self.config_dir.glob("*.json"):
            if file_path.name not in ["default.json", "user.json", "runtime.json"]:
                environments.append(file_path.stem)
        return environments
    
    def export_config(self, output_file: str):
        """Export current configuration to a file."""
        if self.config is None:
            self.load_config()
            
        config_dict = {
            "hardware": asdict(self.config.hardware),
            "performance": asdict(self.config.performance),
            "system": asdict(self.config.system)
        }
        
        output_path = Path(output_file)
        self._save_config_file(output_path, config_dict)
        self.logger.info(f"Configuration exported to {output_file}")
    
    def validate_config(self, config: ApplicationConfig) -> List[str]:
        """Validate configuration and return list of issues."""
        issues = []
        
        try:
            config._validate()
        except ValueError as e:
            issues.append(str(e))
        
        # Additional validation
        if not Path(config.system.data_output_dir).parent.exists():
            issues.append(f"Data output directory parent does not exist: {config.system.data_output_dir}")
        
        if config.system.log_file and not Path(config.system.log_file).parent.exists():
            issues.append(f"Log file directory does not exist: {config.system.log_file}")
        
        return issues

def create_sample_configs():
    """Create sample configuration files for different environments."""
    config_manager = ConfigManager()
    
    # Development environment
    dev_config = {
        "hardware": {
            "shimmer_com_port": "COM3",
            "webcam_index": 0
        },
        "performance": {
            "max_latency_ms": 2.0,
            "monitoring_interval_ms": 50
        },
        "system": {
            "log_level": "DEBUG",
            "log_file": "./logs/development.log"
        }
    }
    config_manager.create_environment_config("development", dev_config)
    
    # Production environment
    prod_config = {
        "hardware": {
            "shimmer_com_port": "COM1",
            "webcam_index": 0
        },
        "performance": {
            "max_latency_ms": 0.5,
            "monitoring_interval_ms": 10
        },
        "system": {
            "log_level": "WARNING",
            "log_file": "./logs/production.log",
            "backup_enabled": True,
            "thread_priority": "high"
        }
    }
    config_manager.create_environment_config("production", prod_config)
    
    # Testing environment
    test_config = {
        "hardware": {
            "shimmer_com_port": "MOCK",
            "webcam_index": -1
        },
        "performance": {
            "max_latency_ms": 5.0,
            "monitoring_interval_ms": 100
        },
        "system": {
            "log_level": "DEBUG",
            "log_file": "./logs/testing.log"
        }
    }
    config_manager.create_environment_config("testing", test_config)
    
    print("✓ Sample configuration files created")
    print(f"  Available environments: {config_manager.list_environments()}")

def main():
    """Demonstrate configuration management capabilities."""
    print("🔧 Advanced Configuration Management Demo")
    print("=" * 50)
    
    # Create sample configurations
    create_sample_configs()
    print()
    
    # Load configuration
    config_manager = ConfigManager()
    config = config_manager.load_config()
    
    print("📋 Current Configuration:")
    print("-" * 30)
    print(f"Environment: {config_manager.environment}")
    print(f"Shimmer COM Port: {config.hardware.shimmer_com_port}")
    print(f"Webcam Resolution: {config.hardware.webcam_width}x{config.hardware.webcam_height}")
    print(f"Max Latency: {config.performance.max_latency_ms} ms")
    print(f"Log Level: {config.system.log_level}")
    print()
    
    # Validate configuration
    issues = config_manager.validate_config(config)
    if issues:
        print("⚠️  Configuration Issues:")
        for issue in issues:
            print(f"  • {issue}")
    else:
        print("✅ Configuration validation passed")
    print()
    
    # Demonstrate runtime updates
    print("🔄 Runtime Configuration Update:")
    config_manager.update_runtime_config({
        "performance": {
            "max_latency_ms": 0.8
        }
    })
    
    updated_config = config_manager.get_config()
    print(f"  Updated max latency: {updated_config.performance.max_latency_ms} ms")
    print()
    
    # Export configuration
    config_manager.export_config("./exported_config.json")
    print("💾 Configuration exported to exported_config.json")
    print()
    
    print("🎯 Configuration Management Features:")
    print("-" * 40)
    print("• Multi-environment support")
    print("• Runtime configuration updates")
    print("• Configuration validation")
    print("• Hierarchical configuration merging")
    print("• Export/import capabilities")
    print("• Environment-specific overrides")
    
    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)