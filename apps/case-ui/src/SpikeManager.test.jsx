import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
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

test('disables Report spike while the request is in flight', async () => {
  let resolveFetch
  const fetchMock = vi.fn().mockReturnValue(
    new Promise((resolve) => {
      resolveFetch = resolve
    }),
  )
  vi.stubGlobal('fetch', fetchMock)

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  const button = screen.getByRole('button', { name: 'Report spike' })
  expect(button.disabled).toBe(false)

  fireEvent.click(button)
  await waitFor(() => {
    expect(button.disabled).toBe(true)
  })

  resolveFetch({
    status: 201,
    json: async () => ({ id: 1, repeated: false, failed_manufacturing_results: [] }),
  })

  expect(await screen.findByText('The spike is reported and stored.')).toBeTruthy()
  await waitFor(() => {
    expect(button.disabled).toBe(false)
  })
})

test('shows that the spike is reported and stored', async () => {
  const fetchMock = vi.fn().mockResolvedValue({
    status: 201,
    json: async () => ({ id: 1, repeated: false, failed_manufacturing_results: [] }),
  })
  vi.stubGlobal('fetch', fetchMock)

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Report spike' }))

  expect(await screen.findByText('The spike is reported and stored.')).toBeTruthy()
  expect(
    screen.queryByText(
      'This spike was already reported. The operator does not need to report it again.',
    ),
  ).toBeNull()
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
    product_name: 'Ford APCU-01',
  })
})

test('shows that this spike was already reported when repeated is true', async () => {
  const fetchMock = vi.fn().mockResolvedValue({
    status: 200,
    json: async () => ({
      id: 1,
      product_name: 'Ford APCU-01',
      repeated: true,
      failed_manufacturing_results: [],
    }),
  })
  vi.stubGlobal('fetch', fetchMock)

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Report spike' }))

  expect(
    await screen.findByText(
      'This spike was already reported. The operator does not need to report it again.',
    ),
  ).toBeTruthy()
  expect(screen.queryByText('The spike is reported and stored.')).toBeNull()
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

  const message = await screen.findByText('no failed part found under Ford APCU-01')
  expect(message).toBeTruthy()
  expect(message.classList.contains('error')).toBe(false)
  expect(screen.queryByText('The spike is reported and stored.')).toBeNull()
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
    product_name: 'Ford APCU-01',
  })
})

test('shows the backend error message when the API returns an error', async () => {
  const fetchMock = vi.fn().mockResolvedValue({
    status: 400,
    json: async () => ({
      message: 'product name is required',
    }),
  })
  vi.stubGlobal('fetch', fetchMock)

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Report spike' }))

  const message = await screen.findByText('product name is required')
  expect(message).toBeTruthy()
  expect(message.classList.contains('error')).toBe(true)
  expect(screen.queryByText('The spike is reported and stored.')).toBeNull()
})

test('shows the copied failed result facts', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      status: 201,
      json: async () => ({
        id: 1,
        product_name: 'Ford APCU-01',
        repeated: false,
        failed_manufacturing_results: [
          {
            id: 9,
            serial_number: 'EXAMPLE-SERIAL',
            test_data_json: { example: '0010' },
            op_code: 'COLD_EOL',
            station_id: 'AEL-01-COLD-EOL',
            rack_id: 'example-rack',
            fixture_id: 'example-fixture',
            nest_number: 1,
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
  expect(tableText).toContain('AEL-01-COLD-EOL')
  expect(tableText).toContain('example-rack')
  expect(tableText).toContain('example-fixture')
  expect(tableText).toContain('1')
  expect(tableText).toContain('example-operator')
  expect(tableText).toContain('example-shift')
  expect(tableText).toContain('2026-11-06T12:00:00Z')
  expect(screen.getByRole('columnheader', { name: 'Station id' })).toBeTruthy()
  expect(screen.getByRole('columnheader', { name: 'Rack id' })).toBeTruthy()
  expect(screen.getByRole('columnheader', { name: 'Fixture id' })).toBeTruthy()
  expect(screen.getByRole('columnheader', { name: 'Nest number' })).toBeTruthy()
})

test('shows a general message when the call fails', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network')))

  render(<SpikeManager />)
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'Ford APCU-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Report spike' }))

  expect(await screen.findByText('The report failed.')).toBeTruthy()
  expect(screen.queryByText('The spike is reported and stored.')).toBeNull()
})

test('shows incomplete response when repeated is missing', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      status: 201,
      json: async () => ({
        id: 1,
        product_name: 'Ford APCU-01',
        failed_manufacturing_results: [
          {
            id: 9,
            serial_number: 'EXAMPLE-SERIAL',
            test_data_json: { example: '0010' },
            op_code: 'COLD_EOL',
            station_id: 'AEL-01-COLD-EOL',
            rack_id: 'example-rack',
            fixture_id: 'example-fixture',
            nest_number: 1,
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

  const message = await screen.findByText(
    'The report response is incomplete. Please contact the support team.',
  )
  expect(message).toBeTruthy()
  expect(message.classList.contains('error')).toBe(true)
  expect(screen.queryByText('The spike is reported and stored.')).toBeNull()
  expect(
    screen.queryByText(
      'This spike was already reported. The operator does not need to report it again.',
    ),
  ).toBeNull()
  expect(screen.queryByRole('table')).toBeNull()
})
