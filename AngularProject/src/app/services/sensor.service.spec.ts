/**
 * sensor.service.spec.ts
 * This file contains unit tests for the SensorService.
 */
import { TestBed } from '@angular/core/testing';

import { SensorService } from './sensor.service';

describe('SensorService', () => {
  let service: SensorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SensorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
