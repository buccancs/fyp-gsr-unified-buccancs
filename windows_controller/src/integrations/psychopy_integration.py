"""
PsychoPy integration for psychological experiments and stimulus presentation.
This module provides functionality to create and run psychological experiments
synchronized with GSR and video recording.
"""

import time
import threading
from typing import Optional, Dict, Any, List, Callable
import logging
import os

try:
    from psychopy import visual, core, event, data, gui, sound
    from psychopy.hardware import keyboard
except ImportError:
    visual = core = event = data = gui = sound = keyboard = None
    logging.warning("PsychoPy not available. Experiment functionality will be disabled.")


class ExperimentController:
    """
    Controls PsychoPy experiments with synchronized data recording.
    """
    
    def __init__(self, window_size: tuple = (1024, 768), fullscreen: bool = False):
        self.logger = logging.getLogger(__name__)
        self.window_size = window_size
        self.fullscreen = fullscreen
        self.window: Optional[visual.Window] = None
        self.clock: Optional[core.Clock] = None
        self.keyboard_handler: Optional[keyboard.Keyboard] = None
        
        # Experiment state
        self.is_running = False
        self.current_trial = 0
        self.experiment_data = []
        
        # Callbacks for synchronization
        self.trial_start_callback: Optional[Callable] = None
        self.trial_end_callback: Optional[Callable] = None
        self.stimulus_onset_callback: Optional[Callable] = None
        self.response_callback: Optional[Callable] = None
        
    def initialize(self) -> bool:
        """
        Initializes the PsychoPy experiment environment.
        
        Returns:
            True if initialization was successful, False otherwise
        """
        if visual is None:
            self.logger.error("PsychoPy not available")
            return False
            
        try:
            # Create window
            self.window = visual.Window(
                size=self.window_size,
                fullscr=self.fullscreen,
                color='black',
                units='pix'
            )
            
            # Create clock for timing
            self.clock = core.Clock()
            
            # Create keyboard handler
            self.keyboard_handler = keyboard.Keyboard()
            
            self.logger.info("PsychoPy experiment environment initialized")
            return True
            
        except Exception as e:
            self.logger.error(f"Error initializing PsychoPy: {e}")
            return False
    
    def create_text_stimulus(self, text: str, pos: tuple = (0, 0), 
                           color: str = 'white', height: int = 48) -> visual.TextStim:
        """
        Creates a text stimulus.
        
        Args:
            text: Text to display
            pos: Position (x, y) in pixels
            color: Text color
            height: Text height in pixels
            
        Returns:
            TextStim object
        """
        return visual.TextStim(
            win=self.window,
            text=text,
            pos=pos,
            color=color,
            height=height
        )
    
    def create_image_stimulus(self, image_path: str, pos: tuple = (0, 0), 
                            size: Optional[tuple] = None) -> visual.ImageStim:
        """
        Creates an image stimulus.
        
        Args:
            image_path: Path to image file
            pos: Position (x, y) in pixels
            size: Size (width, height) in pixels
            
        Returns:
            ImageStim object
        """
        return visual.ImageStim(
            win=self.window,
            image=image_path,
            pos=pos,
            size=size
        )
    
    def create_fixation_cross(self, size: int = 40, color: str = 'white') -> visual.ShapeStim:
        """
        Creates a fixation cross stimulus.
        
        Args:
            size: Size of the cross in pixels
            color: Color of the cross
            
        Returns:
            ShapeStim object representing a fixation cross
        """
        return visual.ShapeStim(
            win=self.window,
            vertices='cross',
            size=size,
            fillColor=color,
            lineColor=color
        )
    
    def show_stimulus(self, stimulus, duration: float = None, 
                     wait_for_response: bool = False) -> Dict[str, Any]:
        """
        Shows a stimulus on screen.
        
        Args:
            stimulus: PsychoPy stimulus object
            duration: Duration to show stimulus (None for indefinite)
            wait_for_response: Whether to wait for keyboard response
            
        Returns:
            Dictionary with timing and response information
        """
        if self.window is None:
            self.logger.error("Window not initialized")
            return {}
        
        # Clear keyboard buffer
        self.keyboard_handler.clearEvents()
        
        # Record onset time
        onset_time = self.clock.getTime()
        
        # Trigger stimulus onset callback
        if self.stimulus_onset_callback:
            self.stimulus_onset_callback('stimulus_onset', onset_time)
        
        # Draw and flip
        stimulus.draw()
        self.window.flip()
        
        response_data = {
            'onset_time': onset_time,
            'response_time': None,
            'response_key': None,
            'reaction_time': None
        }
        
        # Handle duration and response
        if wait_for_response:
            keys = self.keyboard_handler.waitKeys(maxWait=duration)
            if keys:
                response_data['response_key'] = keys[0].name
                response_data['response_time'] = keys[0].rt + onset_time
                response_data['reaction_time'] = keys[0].rt
                
                # Trigger response callback
                if self.response_callback:
                    self.response_callback(keys[0].name, response_data['response_time'])
        
        elif duration is not None:
            core.wait(duration)
        
        return response_data
    
    def show_instruction(self, text: str, continue_key: str = 'space') -> float:
        """
        Shows instruction text and waits for key press.
        
        Args:
            text: Instruction text to display
            continue_key: Key to continue (default: space)
            
        Returns:
            Time when instruction was dismissed
        """
        instruction = self.create_text_stimulus(text)
        instruction.draw()
        
        # Add continue instruction
        continue_text = self.create_text_stimulus(
            f"Press {continue_key} to continue",
            pos=(0, -200),
            height=24
        )
        continue_text.draw()
        
        self.window.flip()
        
        # Wait for key press
        keys = event.waitKeys(keyList=[continue_key])
        return self.clock.getTime()
    
    def run_trial(self, trial_config: Dict[str, Any]) -> Dict[str, Any]:
        """
        Runs a single experimental trial.
        
        Args:
            trial_config: Configuration dictionary for the trial
            
        Returns:
            Trial results dictionary
        """
        trial_start_time = self.clock.getTime()
        
        # Trigger trial start callback
        if self.trial_start_callback:
            self.trial_start_callback(self.current_trial, trial_start_time)
        
        trial_data = {
            'trial_number': self.current_trial,
            'start_time': trial_start_time,
            'config': trial_config,
            'events': []
        }
        
        try:
            # Example trial structure - customize based on your experiment
            
            # 1. Fixation period
            if trial_config.get('fixation_duration', 0) > 0:
                fixation = self.create_fixation_cross()
                fixation_data = self.show_stimulus(
                    fixation, 
                    duration=trial_config['fixation_duration']
                )
                trial_data['events'].append(('fixation', fixation_data))
            
            # 2. Stimulus presentation
            if 'stimulus' in trial_config:
                stimulus_config = trial_config['stimulus']
                
                if stimulus_config['type'] == 'text':
                    stimulus = self.create_text_stimulus(stimulus_config['content'])
                elif stimulus_config['type'] == 'image':
                    stimulus = self.create_image_stimulus(stimulus_config['path'])
                else:
                    raise ValueError(f"Unknown stimulus type: {stimulus_config['type']}")
                
                stimulus_data = self.show_stimulus(
                    stimulus,
                    duration=stimulus_config.get('duration'),
                    wait_for_response=stimulus_config.get('wait_for_response', False)
                )
                trial_data['events'].append(('stimulus', stimulus_data))
            
            # 3. Inter-trial interval
            if trial_config.get('iti_duration', 0) > 0:
                core.wait(trial_config['iti_duration'])
            
        except Exception as e:
            self.logger.error(f"Error in trial {self.current_trial}: {e}")
            trial_data['error'] = str(e)
        
        trial_end_time = self.clock.getTime()
        trial_data['end_time'] = trial_end_time
        trial_data['duration'] = trial_end_time - trial_start_time
        
        # Trigger trial end callback
        if self.trial_end_callback:
            self.trial_end_callback(self.current_trial, trial_end_time)
        
        self.current_trial += 1
        self.experiment_data.append(trial_data)
        
        return trial_data
    
    def run_experiment(self, trial_list: List[Dict[str, Any]], 
                      instructions: str = None) -> List[Dict[str, Any]]:
        """
        Runs a complete experiment.
        
        Args:
            trial_list: List of trial configurations
            instructions: Optional instruction text
            
        Returns:
            List of trial results
        """
        if self.window is None:
            self.logger.error("Window not initialized")
            return []
        
        self.is_running = True
        self.current_trial = 0
        self.experiment_data = []
        
        try:
            # Show instructions if provided
            if instructions:
                self.show_instruction(instructions)
            
            # Run trials
            for trial_config in trial_list:
                if not self.is_running:
                    break
                
                # Check for escape key to abort
                keys = event.getKeys(keyList=['escape'])
                if keys:
                    self.logger.info("Experiment aborted by user")
                    break
                
                trial_data = self.run_trial(trial_config)
                self.logger.info(f"Completed trial {trial_data['trial_number']}")
            
            # Show completion message
            completion_text = "Experiment completed. Thank you!"
            self.show_instruction(completion_text, continue_key='space')
            
        except Exception as e:
            self.logger.error(f"Error running experiment: {e}")
        
        finally:
            self.is_running = False
        
        return self.experiment_data
    
    def set_trial_start_callback(self, callback: Callable):
        """Sets callback for trial start events."""
        self.trial_start_callback = callback
    
    def set_trial_end_callback(self, callback: Callable):
        """Sets callback for trial end events."""
        self.trial_end_callback = callback
    
    def set_stimulus_onset_callback(self, callback: Callable):
        """Sets callback for stimulus onset events."""
        self.stimulus_onset_callback = callback
    
    def set_response_callback(self, callback: Callable):
        """Sets callback for response events."""
        self.response_callback = callback
    
    def stop_experiment(self):
        """Stops the currently running experiment."""
        self.is_running = False
    
    def save_data(self, filename: str, format: str = 'csv'):
        """
        Saves experiment data to file.
        
        Args:
            filename: Output filename
            format: Data format ('csv', 'json', 'pickle')
        """
        try:
            if format == 'csv':
                # Flatten data for CSV export
                import pandas as pd
                flattened_data = []
                
                for trial in self.experiment_data:
                    row = {
                        'trial_number': trial['trial_number'],
                        'start_time': trial['start_time'],
                        'end_time': trial['end_time'],
                        'duration': trial['duration']
                    }
                    
                    # Add event data
                    for event_name, event_data in trial.get('events', []):
                        for key, value in event_data.items():
                            row[f'{event_name}_{key}'] = value
                    
                    flattened_data.append(row)
                
                df = pd.DataFrame(flattened_data)
                df.to_csv(filename, index=False)
                
            elif format == 'json':
                import json
                with open(filename, 'w') as f:
                    json.dump(self.experiment_data, f, indent=2)
                    
            elif format == 'pickle':
                import pickle
                with open(filename, 'wb') as f:
                    pickle.dump(self.experiment_data, f)
            
            self.logger.info(f"Experiment data saved to {filename}")
            
        except Exception as e:
            self.logger.error(f"Error saving data: {e}")
    
    def cleanup(self):
        """Cleans up PsychoPy resources."""
        if self.window:
            self.window.close()
        core.quit()


class StimulusLibrary:
    """
    Library of common experimental stimuli.
    """
    
    @staticmethod
    def create_stroop_stimuli(window) -> List[visual.TextStim]:
        """Creates Stroop task stimuli."""
        colors = ['red', 'green', 'blue', 'yellow']
        words = ['RED', 'GREEN', 'BLUE', 'YELLOW']
        
        stimuli = []
        for word in words:
            for color in colors:
                stimulus = visual.TextStim(
                    win=window,
                    text=word,
                    color=color,
                    height=60
                )
                stimuli.append({
                    'stimulus': stimulus,
                    'word': word,
                    'color': color,
                    'congruent': word.lower() == color
                })
        
        return stimuli
    
    @staticmethod
    def create_emotional_faces(window, image_directory: str) -> List[Dict]:
        """Creates emotional face stimuli from image directory."""
        stimuli = []
        
        if not os.path.exists(image_directory):
            return stimuli
        
        for filename in os.listdir(image_directory):
            if filename.lower().endswith(('.png', '.jpg', '.jpeg')):
                image_path = os.path.join(image_directory, filename)
                stimulus = visual.ImageStim(
                    win=window,
                    image=image_path,
                    size=(400, 400)
                )
                
                # Extract emotion from filename (assuming format: emotion_id.jpg)
                emotion = filename.split('_')[0] if '_' in filename else 'neutral'
                
                stimuli.append({
                    'stimulus': stimulus,
                    'emotion': emotion,
                    'filename': filename
                })
        
        return stimuli