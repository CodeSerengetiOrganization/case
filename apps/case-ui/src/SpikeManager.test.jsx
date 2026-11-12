import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import { SpikeManager } from './SpikeManager'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

test('starts with no product name and a gray Report spike button', () => {
  render(<SpikeManager />)

  expect(screen.getByRole('combobox').value).toBe('')
  expect(screen.getByRole('button', { name: 'Report spike' }).disabled).toBe(true)
})

test('shows that the spike is reported and stored', async () => {
  const fetchMock = vi.fn().mockResolvedValue({
    status: 200,
    json: async () => ({ id: 1, failed_manufacturing_results: [] }),
  })
  vi.stubGlobal('fetch', fetchMock)

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Report spike' }))

  expect(await screen.findByText('The spike is reported and stored.')).toBeTruthy()
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
    product_name: 'Ford APCU-01',
  })
})

test('shows the message when the search finds no failed part', async () => {
  const fetchMock = vi.fn().mockResolvedValue({
    status: 200,
    json: async () => ({
      product_name: 'Ford APCU-01',
      message: 'no failed part found under Ford APCU-01',
    }),
  })
  vi.stubGlobal('fetch', fetchMock)

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Report spike' }))

  expect(await screen.findByText('no failed part found under Ford APCU-01')).toBeTruthy()
  expect(screen.queryByText('The spike is reported and stored.')).toBeNull()
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
    product_name: 'Ford APCU-01',
  })
})

test('shows the copied failed result facts', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      status: 200,
      json: async () => ({
        id: 1,
        product_name: 'Ford APCU-01',
        failed_manufacturing_results: [
          {
            id: 9,
            serial_number: 'EXAMPLE-SERIAL',
            test_data_json: { example: '0010' },
            op_code: 'COLD_EOL',
            station_setup: {
              station_id: 'AEL-01-COLD-EOL',
              rack_id: 'example-rack',
              fixture_id: 'example-fixture',
              nest_number: 1,
            },
            operator_id: 'example-operator',
            shift_code: 'example-shift',
            created_at: '2026-11-06T12:00:00Z',
          },
        ],
      }),
    }),
  )

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Report spike' }))

  expect(await screen.findByRole('table')).toBeTruthy()
  const tableText = screen.getByRole('table').textContent
  expect(tableText).toContain('Ford APCU-01')
  expect(tableText).toContain('EXAMPLE-SERIAL')
  expect(tableText).toContain('example')
  expect(tableText).toContain('COLD_EOL')
  expect(tableText).toContain('AEL-01-COLD-EOL / example-rack / example-fixture / 1')
  expect(tableText).toContain('example-operator')
  expect(tableText).toContain('example-shift')
  expect(tableText).toContain('2026-11-06T12:00:00Z')
})

test('shows a general message when the call fails', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network')))

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Report spike' }))

  expect(await screen.findByText('The report failed.')).toBeTruthy()
  expect(screen.queryByText('The spike is reported and stored.')).toBeNull()
})
